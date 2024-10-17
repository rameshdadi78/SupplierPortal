node {
    def tomcatWeb = 'E:\apache-tomee-supplier\webapps'
    def tomcatBin = 'E:\apache-tomee-supplier\bin'
    
    stage('SCM Checkout') {
        git 'https://github.com/ashVen1996/SupplierPortal.git'
    }
    
    stage('Compile-Package-create-war-file') {
        def mvnHome = tool name: 'maven-3.9.9', type: 'maven'
        bat "${mvnHome}\\bin\\mvn package"
    }
    
    stage('Stop Tomcat Server') {
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
    
    stage('Deploy to Tomcat') {
        bat "copy target\\JenkinsWar.war ${tomcatWeb}\\JenkinsWar.war"
    }
    
    stage('Start Tomcat Server') {
        bat "${tomcatBin}\\startup.bat"
        timeout time: 5, unit: 'SECONDS'
    }
}
