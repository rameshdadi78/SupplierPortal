pipeline {
    agent any

    environment {
        NODE_HOME = tool name: 'nodejs', type: 'nodejs'
        REACT_APP_DIR = 'E:\\supplier' // Adjust this path to your React app
        tomcatWeb = 'E:\\apache-tomee-supplier\\webapps'
        tomcatBin = 'E:\\apache-tomee-supplier\\bin'
    }

    stages {
        stage('SCM Checkout - Java') {
            steps {
                git 'https://github.com/rameshdadi78/SupplierPortal.git'
            }
        }

        stage('Compile-Package-create-war-file') {
            steps {
                script {
                    def mvnHome = tool name: 'maven-3.9.9', type: 'maven'
                    bat "${mvnHome}\\bin\\mvn package"
                }
            }
        }

        stage('SCM Checkout - React') {
            steps {
                git 'https://github.com/rameshdadi78/SupplierPortal.git'
            }
        }

        stage('Install Dependencies') {
            steps {
                dir(REACT_APP_DIR) {
                    bat "${NODE_HOME}\\npm install"
                }
            }
        }

        stage('Build React App') {
            steps {
                dir(REACT_APP_DIR) {
                    bat "${NODE_HOME}\\npm run build"
                }
            }
        }

        stage('Stop Tomcat Server') {
            steps {
                bat """
                tasklist | findstr /I "tomcat" >nul
                if %ERRORLEVEL% == 0 (
                    echo Tomcat is running. Stopping Tomcat...
                    cd ${tomcatBin}
                    shutdown.bat
                    timeout /t 10
                ) else (
                    echo Tomcat is not running.
                )
                """
            }
        }

        stage('Deploy React App') {
            steps {
                echo 'Deploying the React app...'
                bat "xcopy ${REACT_APP_DIR}\\build\\* ${tomcatWeb}\\ROOT\\ /E /I"
            }
        }

        stage('Deploy Java App to Tomcat') {
            steps {
                bat "copy target\\JenkinsWar.war ${tomcatWeb}\\JenkinsWar.war"
            }
        }

        stage('Start Tomcat Server') {
            steps {
                bat "${tomcatBin}\\startup.bat"
                timeout time: 5, unit: 'SECONDS'
            }
        }
    }

    post {
        success {
            echo 'Build and deployment successful!'
        }
        failure {
            echo 'Build or deployment failed.'
        }
    }
}
