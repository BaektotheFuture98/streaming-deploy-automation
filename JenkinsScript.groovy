import groovy.json.JsonSlurperClassic

pipeline {
    agent any
    environment {
        ELASTICSEARCH_HOSTS='"192.168.125.61","192.168.125.62"'
        SCHEMA_REGISTRY="http://192.168.125.61:8081"
        KAFKA_BOOTSTRAP_SERVERS="192.168.125.61:9092"
        ELASTICSEARCH_USER="elastic"
        ELASTICSEARCH_PASSWORD="elastic"
        MYSQL_HOST="jdbc:mysql://192.168.125.61:3306/TEST"
        MYSQL_USER="root"
        MYSQL_PASSWORD="dkfdptmdps404"
        KAFKASTORE_BOOTSTRAP_SERVERS="PLAINTEXT://192.168.125.61:9092,PLAINTEXT://192.168.125.62:9092"
        LISTENERS="http://0.0.0.0:8081"
        ADVERTISED_LISTENERS="http://0.0.0.0:8081"
        CONNECT_BOOTSTRAP_SERVERS="http://192.168.125.61:8083"
        LOGSTASH_SERVER="192.168.25.24"
        LOGSTASH_HOME="/seonmin/logstash-9.1.2"
    }
    stages {
        stage("GitHub WebHook") {
            steps {
                git branch: 'main',
                    credentialsId: 'github_personal_access_token',
                    url: 'https://github.com/BaektotheFuture98/streaming-deploy-automation.git'
            }
        }

        stage("Distribute Settings File") {
            steps {
                script {
                    def services = sh(
                        script: "bash deploy.sh", 
                        returnStdout: true
                    ).trim().split("\n")

                    services.each { service -> 
                        echo "▶ Deploying ${service}"
                        def path = "./${service}"

                        // deploySchema(path)
                        deployLogstash(path, service)
                        deployConnector(path)
                    }
                }
            }
        }
    }
}

def deployLogstash(path, service) {
    def logstashFile = "${path}/Logstash/logstash.conf"
    if (!fileExists(logstashFile)) return
    
    def fileChnaged = sh(
            script: "git diff --name-only HEAD~1 HEAD | grep 'logstash.conf' || true", 
            returnStdout: true
        ).trim()

    if (fileChnaged) {
        echo "✔ Logstash config changed: ${fileChnaged}"
        sh "envsubst < ${path}/Logstash/logstash.conf > ${path}/Logstash/logstash-tmp.conf && touch ${path}/Logstash/${service}-logstash.conf && mv ${path}/Logstash/logstash-tmp.conf ${path}/Logstash/${service}-logstash.conf"
        sh "cp ${path}/Logstash/${service}-logstash.conf ${LOGSTASH_HOME}/config/"
    } else {
        echo "✘ No logstash config changes detected."   
    }
}

def deployConnector(path) {
    def connectorFile = "${path}/Connector/connector.json"
    if (!fileExists(connectorFile)) return 

    def fileChanged = sh(
        script: "git diff --name-only HEAD~1 HEAD | grep 'connector.json' || true",
        returnStdout: true 
    ).trim()

    if (fileChanged) {
        echo "✔ Connector config changed: ${fileChanged}"
        sh "envsubst < ${connectorFile} > ${connectorFile}.tmp && mv ${connectorFile}.tmp ${connectorFile}"
        sh "curl -X POST -H 'Content-Type: application/json' --data-binary @${connectorFile} ${CONNECT_BOOTSTRAP_SERVERS}/connectors"
    } else {
        echo "✘ No connector config changes detected."
    }
}