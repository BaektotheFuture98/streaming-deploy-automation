import groovy.json.JsonSlurper

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
        stage("checkout") {
            steps {
                git branch: 'main',
                    credentialsId: 'github_personal_access_token',
                    url: 'https://github.com/BaektotheFuture98/streaming-deploy-automation.git'
            }
        }

        stage("deploy") {
            steps {
                script {
                    def services = sh(
                        script: "bash deploy.sh", 
                        returnStdout: true
                    ).trim().split("\n")

                    services.each { service -> 
                        echo "▶ Deploying ${service}"
                        def path = "./${service}"

                        deploySchema(path)
                        deployLogstash(path, service)
                        deployConnector(path)
                    }
                }
            }
        }

        stage("Test") {
            steps {
                script
            }
        }
    }
}


def deploySchema(path) {
    def schemaFile = "${path}/SchemaRegistry/schema.json"
    
    jsonfile.name = env
    if (!fileExists(schemaFile)) return 

    def schema_raw_file = readJSON file : schemaFile
    def schema_file = schemaFile
    echo "Schemafile : ${schema_file}"
    env.SCHEMA_NAME = schema_raw_file['name']
    env.SCHEMA_ID = schema_raw_file['schema_id']
    def fileChanged = sh(
        script: "git diff --name-only HEAD~1 HEAD | grep 'schema.json' || true",
        returnStdout: true
    ).trim()

    if (fileChanged) {
        echo "✔ Schema changed: ${fileChanged}"
        sh """
            curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
            --data ${string_data}
        """
    } else {
        echo "✘ No schema changes detected."
    }
}

def deployLogstash(path, service) {
    def logstashFile = "${path}/Logstash/${service}-logstash.conf"
    if (!fileExists(logstashFile)) return
    
    def fileChnaged = sh(
            script: "git diff --name-only HEAD~1 HEAD | grep '${service}-logstash.conf' || true", 
            returnStdout: true
        ).trim()

    if (fileChnaged) {
        echo "✔ Logstash config changed: ${fileChnaged}"
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
        sh "curl -X POST -H 'Content-Type: application/json' --data-binary @${connectorFile} ${CONNECT_BOOTSTRAP_SERVERS}/connectors"
    } else {
        echo "✘ No connector config changes detected."
    }
}
