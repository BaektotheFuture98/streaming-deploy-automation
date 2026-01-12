import groovy.json.JsonOutput

pipeline {
    agent any
    environment {
        PATH="/seonmin/Python-3.13.7:$PATH"
        PYTHON_HOME="/seonmin/Python-3.13.7/python"

        ELASTICSEARCH_HOSTS='"PLT-DEV-01","PLT-DEV-02"'
        SCHEMA_REGISTRY="http://PLT-DEV-01:8081"
        KAFKA_BOOTSTRAP_SERVERS="PLT-DEV-02:9092"

        ELASTICSEARCH_USER="elastic"
        ELASTICSEARCH_PASSWORD="****"

        MYSQL_HOST="jdbc:mysql://PLT-DEV-01:3306/TEST"
        MYSQL_USER="root"
        MYSQL_PASSWORD="****"

        KAFKASTORE_BOOTSTRAP_SERVERS="PLAINTEXT://PLT-DEV-01:9092,PLAINTEXT://PLT-DEV-02:9092"
        LOGSTASH_SERVER="192.168.25.24"
        LOGSTASH_HOME="/seonmin/logstash-9.1.2"
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
                        script: "bash ./modules/deploy.sh",
                        returnStdout: true
                    ).trim().split("\n")

                    echo "📦 서비스 목록: ${services}"

                    services.each { service ->
                        echo "▶ [${service}] 배포 시작"
                        def basePath = "./modules/EsToDB"

                        setEnvFile(basePath, service)
                        def schemaId = deploySchemaRegistry(basePath, service)
                        deployLogstash(basePath, service, schemaId)
                        deployConnector(basePath, service)

                        echo "✅ [${service}] 배포 완료"
                        startLogstash(service)
                    }
                }
            }
        }
    }
}


def startLogstash(service) {
    echo "===== 🚀 Logstash 실행 시작 ====="
    sh """
        ${LOGSTASH_HOME}/bin/logstash -f ${LOGSTASH_HOME}/config/${service}-logstash.conf
    """
}


def setEnvFile(path, service) {
    echo "📄 setEnvFile() - ${service}"

    def paramFile = "${path}/Services/${service}/${service}.json"
    def paramJson = readJSON file: paramFile

    paramJson.each { key, value ->
        env."${key}" = (value instanceof Map) ? JsonOutput.toJson(value) : value.toString()
    }

    echo "✅ 환경변수 로드 완료"
}


def deploySchemaRegistry(path, service) {
    echo "📄 deploySchemaRegistry()"

    def schemaFile = "${path}/Services/${service}/schema.json"
    sh "envsubst < ${schemaFile} > ${schemaFile}.tmp && mv ${schemaFile}.tmp ${schemaFile}"

    def deployScript = "${path}/SchemaRegistry/PostSchemaRegistry.py"

    sh "python -m pip install -r ${path}/SchemaRegistry/requirements.txt"

    def schemaId = sh(script: "python ${deployScript}", returnStdout: true).trim()
    echo "✅ Schema Registry 등록 완료: ${schemaId}"

    return schemaId
}


def deployLogstash(path, service, schemaId) {		
    echo "📄 deployLogstash() - ${service}"

    def template = "${path}/Logstash/logstash.conf"
    sh """
        envsubst < ${template} > ${path}/Logstash/${service}-logstash.conf
        cp ${path}/Logstash/${service}-logstash.conf ${LOGSTASH_HOME}/config/
    """

    echo "✅ Logstash 설정 반영 완료"
}


def deployConnector(path, service) {
    echo "📄 deployConnector() - ${service}"

    def connectorFile = "${path}/Connector/connector.json"
    if (!fileExists(connectorFile)) {
        echo "⚠️ Connector 설정 없음 — Skip"
        return
    }

    sh "envsubst < ${connectorFile} > ${connectorFile}.tmp && mv ${connectorFile}.tmp ${connectorFile}"

    def exists = sh(script: "curl -s ${CONNECT_BOOTSTRAP_SERVERS}/connectors || true", returnStdout: true)

    if (exists.contains("${service}-SinkConnector")) {
        echo "♻️ Connector 업데이트"
        sh """
            curl -X PUT -H 'Content-Type: application/json' \
            --data-binary @${connectorFile} \
            ${CONNECT_BOOTSTRAP_SERVERS}/connectors/${service}-SinkConnector/config
        """
    } else {
        echo "🆕 Connector 새 등록"
        sh """
            curl -X POST -H 'Content-Type: application/json' \
            --data-binary @${connectorFile} \
            ${CONNECT_BOOTSTRAP_SERVERS}/connectors
        """
    }

    echo "✅ deployConnector() 완료"
}
