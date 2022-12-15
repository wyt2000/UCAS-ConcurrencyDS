#!/bin/sh

javac -encoding UTF-8 -cp . ticketingsystem/GenerateHistory.java
javac -encoding UTF-8 -cp . ticketingsystem/Test.java
java -cp . ticketingsystem/Test 4 10000 
java -cp . ticketingsystem/Test 8 10000 
java -cp . ticketingsystem/Test 16 10000 
java -cp . ticketingsystem/Test 32 10000 
java -cp . ticketingsystem/Test 64 10000 
