#!/bin/sh

javac -encoding UTF-8 -cp . ticketingsystem/GenerateHistory.java
javac -encoding UTF-8 -cp . ticketingsystem/Test.java
java -cp . ticketingsystem/Test 64 10000 
