#mvn --batch-mode clean compile test package gpg:sign deploy
mvn clean compile test package javadoc:aggregate-jar deploy
