# Download maven-toolbox
curl -L http://bit.ly/maven-toolbox | bash
chmod +x @ci/maven-toolbox
chmod +x src/main/webapp/WEB-INF/cwebp

# Configure and build project
./@ci/maven-toolbox configure-properties src/main/resources/config.properties
./@ci/maven-toolbox configure-m2
./@ci/maven-toolbox configure-hibernate
./@ci/maven-toolbox configure-pre-sdk
mvn clean install
./@ci/maven-toolbox configure-sdk
mvn -f target/sdk/pom.xml deploy
