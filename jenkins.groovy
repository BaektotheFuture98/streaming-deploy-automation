import groovy.json.JsonOutput

pipeline {
    agent any
    environment {
        PATH="/seonmin/Python-3.13.7:$PATH"
        PYTHON_HOME="/seonmin/Python-3.13.7/python"
        ELASTICSEARCH_HOSTS='"es-node-01.example.internal","es-node-02.example.internal"'
        SCHEMA_REGISTRY="http://schema-registry.example.internal:8081"
        KAFKA_BOOTSTRAP_SERVERS="kafka-01.example.internal:9092"
        ELASTICSEARCH_USER="CHANGE_ME"
        ELASTICSEARCH_PASSWORD="CHANGE_ME"
        MYSQL_HOST="jdbc:mysql://mysql.example.internal:3306/APP_DB"
        MYSQL_USER="CHANGE_ME"
        MYSQL_PASSWORD="CHANGE_ME"
        KAFKASTORE_BOOTSTRAP_SERVERS="PLAINTEXT://kafka-01.example.internal:9092,PLAINTEXT://kafka-02.example.internal:9092"
        LISTENERS="http://0.0.0.0:8081"
        ADVERTISED_LISTENERS="http://schema-registry.example.internal:8081"
        CONNECT_BOOTSTRAP_SERVERS="http://connect.example.internal:8083"
        LOGSTASH_SERVER="logstash.example.internal"
        LOGSTASH_HOME="/opt/logstash"
    }
    stages {

        stage("GitHub WebHook") {
            steps {
                echo "===== 🌀 GitHub WebHook Stage 시작 ====="
                git branch: 'main',
                    credentialsId: 'github_personal_access_token',
                    url: 'https://github.com/BaektotheFuture98/streaming-deploy-automation.git'
                echo "✅ Git Clone 완료"
            }
        }

        stage("Distribute Settings File") {
            steps {
                script {
                    echo "===== ⚙️ Settings File Distribution 시작 ====="

                    def services = sh(
                        script: "bash deploy.sh", 
                        returnStdout: true
                    ).trim().split("\n")

                    echo "📦 서비스 목록: ${services}"

                    services.each { service -> 
                        echo "▶ [${service}] 배포 시작"
                        def path_to_service = "./EsToDB"

                        echo "📂 경로: ${path}"
                        setEnvFile(path_to_service, service)

                        deploySchemaRegistry(path_to_service)

                        deployLogstash(path_to_service, service)
                        
                        deployConnector(path_to_service, service)

                        echo "✅ [${service}] 배포 완료"
                    }

                    echo "===== ⚙️ Settings File Distribution 완료 ====="
                }
            }
        }

        stage("Start Logstash") { 
            steps {
                echo "===== 🚀 Logstash 실행 시작 ====="
                sh """
                    echo 'Logstash 실행 경로: ${LOGSTASH_HOME}'
                    echo '서비스 설정 파일: ${SERVICE}'
                    ${LOGSTASH_HOME}/bin/logstash -f ${LOGSTASH_HOME}/config/${SERVICE}.conf
                """
                echo "✅ Logstash 실행 완료"
            }
        }
    }
}

def setEnvFile(path_to_service, service) {
    echo "📄 setEnvFile() 시작 - ${service}"

    def paramFile = "${path_to_service}/Services/${service}.json"
    echo "🧾 환경파일 경로: ${paramFile}"

    if (!fileExists(paramFile)) {
        error "❌ ${paramFile} 파일이 존재하지 않습니다."
    }

    def paramJson = readJSON file: paramFile
    echo "🧮 JSON 내용: ${paramJson}"

    paramJson.each { key, value ->
        if (value instanceof Map) {
            // ✅ Map 타입인 경우 (ex. QUERY)
            def jsonString = JsonOutput.toJson(value)
            env."${key}" = jsonString
            echo "🔧 환경변수 등록(JSON): ${key} = ${jsonString}"
        } else {
            env."${key}" = value.toString()
            echo "🔧 환경변수 등록: ${key} = ${value}"
        }
    }
    
    echo "✅ setEnvFile() 완료"
}

def deploySchemaRegistry(path_to_service) {
    echo "📄 deploySchemaRegistry() 시작"

    def schemaFile = "${path_to_service}/SchemaRegistry/schema.json"
    echo "📁 스키마 파일: ${schemaFile}"

    if (!fileExists(schemaFile)) {
        error "❌ ${schemaFile} 파일이 존재하지 않습니다."
    }

    sh """
        echo '🔧 envsubst 치환 중...'
        envsubst < ${schemaFile} > ${schemaFile}.tmp
        mv ${schemaFile}.tmp ${schemaFile}
    """

    def deploySchema = "${path_to_service}/SchemaRegistry/PostSchemaRegistry.py"
    echo "🐍 Python 실행 파일: ${deploySchema}"
    
    sh """
        python -m pip install -r ./EsToDB/SchemaRegistry/requirements.txt
    """
    
    def schemaId = sh(
        script: "python ${deploySchema}",
        returnStdout: true
    ).trim()

    env.SCHEMA_ID = schemaId
    echo "✅ Schema Registry 등록 완료 - SCHEMA_ID: ${env.SCHEMA_ID}"

    return schemaId
}


def deployLogstash(path_to_service, service) {		
    echo "📄 deployLogstash() 시작 - ${service}"
    def logstashFile = "${path_to_service}/Logstash/logstash.conf"

    if (!fileExists(logstashFile)) {
        error "❌ ${logstashFile} 파일이 존재하지 않습니다."
    }

    sh """
        echo '🔧 Logstash 설정파일 envsubst 적용 중...'
        envsubst < ${logstashFile} > ${logstashFile}.tmp
        mv ${logstashFile}.tmp ${path_to_service}/Logstash/${service}-logstash.conf
        cp ${path_to_service}/Logstash/${service}-logstash.conf ${LOGSTASH_HOME}/config/
        echo '✅ Logstash 설정 배포 완료: ${LOGSTASH_HOME}/config/${service}-logstash.conf'
    """
    echo "✅ deployLogstash() 완료"
}


def deployConnector(path_to_service, service){
    echo "📄 deployConnector() 시작 - ${service}"

    def connectorFile = "${path_to_service}/Connector/connector.json"
    if (!fileExists(connectorFile)) {
        echo "⚠️ Connector 파일이 없어 건너뜁니다: ${connectorFile}"
        return
    }

    echo "🧾 Connector 파일: ${connectorFile}"

    def fileChanged = sh(
        script: "git diff --name-only HEAD~1 HEAD | grep 'connector.json' || true",
        returnStdout: true
    ).trim()

    echo "📊 변경된 파일: ${fileChanged}"

    def connectorsResponse = sh(
        script: "curl -s ${CONNECT_BOOTSTRAP_SERVERS}/connectors || true",
        returnStdout: true
    ).trim()

    echo "🌐 Kafka Connect 응답: ${connectorsResponse}"

    if (connectorsResponse.contains("${service}-SinkConnector")) {
        echo "♻️ 기존 Connector 업데이트"
        sh """
            envsubst < ${connectorFile} > ${connectorFile}.tmp
            mv ${connectorFile}.tmp ${connectorFile}
            curl -X PUT -H 'Content-Type: application/json' \
                --data-binary @${connectorFile} \
                ${CONNECT_BOOTSTRAP_SERVERS}/connectors/${service}-SinkConnector/config
        """
    } else {
        echo "🆕 신규 Connector 등록"
        sh """
            envsubst < ${connectorFile} > ${connectorFile}.tmp
            mv ${connectorFile}.tmp ${connectorFile}
            curl -X POST -H 'Content-Type: application/json' \
                --data-binary @${connectorFile} \
                ${CONNECT_BOOTSTRAP_SERVERS}/connectors
        """
    }

    echo "✅ deployConnector() 완료"
}
