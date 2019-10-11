
/* Create a WIFI Server in ESP32 with IP: 192.168.4.1  by default
 * Port 80 by default
 * 
 * You just need to connect to ESP32 Access Point
 * password = "your-password"
 * 
 * Then, you can reprogram ESP32 via IP at Arduino IDE
 * 
 * SENDING COMMNADS
 * To send commands to Node via Browser, just type:
 * 192.168.4.1/ligaled   and wait for the reply
 * 192.168.4.1/motor?speed=25     sends number 25
 * 
 * To send commands to Nede via Terminal, just type:
 * curl 192.168.4.1/ligaled   and wait for the reply
 * curl 192.168.4.1/motor?speed=25     sends number 25
 * 
 * To send more than one argument:
 * 192.168.4.1/motor?speed1=1023&speed2=1023&dir1=1&dir2=0
 * 
 * 
 * Modulo PonteH L298n:     IN1     IN2
 *              Horario     1         0
 *              Anti-Hor    0         1
 *              Ponto Morto 0         0
 *              Freio       1         1
 * 
 * PWM ==> 70 a 255
 * 
 * 
 * 
 * 
 * 
 */


#define LED_BUILTIN 2
#include <ArduinoOTA.h>   // Library to allow programing via WIFI (Choose ip port in Arduino IDE
#include <WiFi.h>        // Include the Wi-Fi library
#include <WebServer.h>
#include <analogWrite.h>  // Aparently, you can analogRead, but not analogWrite without a library with ESP32 !!!
#include <Ultrasonic.h>

#define TRIGGER1 23
#define ECHO1    22
#define TRIGGER2 32
#define ECHO2    33

#define IN1 21
#define IN2 19
#define IN3 18
#define IN4 5
#define THRESHOLD1 30
#define THRESHOLD2 30

int dir1 = 0;
int dir2 = 0;
int speed1 = 0;
int speed2 = 0;
int count = 0;
int parado = 1;

enum { 
  direita, esquerda, frente, para
};

Ultrasonic Sonar1(TRIGGER1, ECHO1, 20000UL);
Ultrasonic Sonar2(TRIGGER2, ECHO2, 20000UL);

unsigned char comando = para;

unsigned char s1 = 0, s2 = 0;

const char *ssid = "ESP32 Access Point"; // The name of the Wi-Fi network that will be created
const char *password = "your-password";   // The password required to connect to it, leave blank for an open

WebServer server(80);

void handleRoot() {
  server.send(200, "text/plain", "XUPA FEDERAL!");
}

void handleNotFound() {
  server.send(404, "text/plain", "Banana");
}

void motor() {
  if(server.hasArg("speed1")) {
    speed1 = server.arg("speed1").toInt();
    speed2 = server.arg("speed2").toInt();

    dir1 = server.arg("dir1").toInt();
    dir2 = server.arg("dir2").toInt();

    Serial.println("Motor 1:");
    Serial.println(speed1);
    Serial.println(dir1);

    Serial.println("Motor 2:");
    Serial.println(speed2);
    Serial.println(dir2);


    if(dir1 == 0)
    {   analogWrite(IN1, speed1);
        analogWrite(IN2, 0);
    }
    else
    {   analogWrite(IN2, speed1);
        analogWrite(IN1, 0);
    }
    
    if(dir2 == 0)
    {   analogWrite(IN3, speed2);
        analogWrite(IN4, 0);
    }
    else
    {   analogWrite(IN4, speed2);
        analogWrite(IN3, 0);
    }
     
    server.send(200, "text/html", "Ok!");
    count = 0;
    parado = 0;

  }
}

void halt() {
    analogWrite(IN1, 0);
    analogWrite(IN2, 0);
    analogWrite(IN3, 0);
    analogWrite(IN4, 0);

    speed1 = 0;
    speed2 = 0;
  
    count = 0;
    parado = 1;

    server.send(200, "text/html", "Ok Parou!!!");
    Serial.println("OK Parou!!!");
}

void sonar1() {
  int distanciaCM;
  distanciaCM = Sonar1.read(CM);

  Serial.print("Distancia1: ");
  Serial.println(distanciaCM);

  if(distanciaCM < THRESHOLD1) s1 = 1;
  else s1 = 0;
  
  char texto[20];
  sprintf(texto, "Distancia1 = %d", (int) distanciaCM);
  server.send(200, "text/html", texto); 
}

void sonar2() {
  int distanciaCM;
  distanciaCM = Sonar2.read(CM);

  Serial.print("Distancia2: ");
  Serial.println(distanciaCM);

  if(distanciaCM < THRESHOLD2) s2 = 1;
  else s2 = 0;

  char texto[20];
  sprintf(texto, "Distancia1 = %d", (int) distanciaCM);
  server.send(200, "text/html", texto); 
}

/*
void sonar1() {
    long duration, distance;
    
    digitalWrite(TRIGGER1, LOW);  
    delayMicroseconds(2); 
    
    digitalWrite(TRIGGER1, HIGH);
    delayMicroseconds(10); 
    
    digitalWrite(TRIGGER1, LOW);
    duration = pulseIn(ECHO1, HIGH);
    distance = (duration/2) / 29.1;

    if(distance < THRESHOLD1) s1 = 1;
    else s1 = 0;
    
    
    Serial.print("Centimeter1: ");
    Serial.println(distance);
    Serial.print("s1 = ");
    Serial.println(s1);

    char texto[20];
    sprintf(texto, "Distancia1 = %d", (int) distance);
    //server.send(200, "text/html", texto);   
}

void sonar2() {
    long duration, distance;
    
    digitalWrite(TRIGGER2, LOW);  
    delayMicroseconds(2); 
    
    digitalWrite(TRIGGER2, HIGH);
    delayMicroseconds(10); 
    
    digitalWrite(TRIGGER2, LOW);
    duration = pulseIn(ECHO2, HIGH);
    distance = (duration/2) / 29.1;

    if(distance < THRESHOLD2) s2 = 1;
    else s2 = 0;
    
    Serial.print("Centimeter2:");
    Serial.println(distance);
    Serial.print("s2 = ");
    Serial.println(s2);

    char texto[20];
    sprintf(texto, "Distancia2 = %d", (int) distance);
    //server.send(200, "text/html", texto);
}
*/

void setup() {
  //SONARES
  pinMode(TRIGGER1, OUTPUT);
  pinMode(ECHO1, INPUT);
  pinMode(TRIGGER2, OUTPUT);
  pinMode(ECHO2, INPUT);
  
  //LED
  pinMode(LED_BUILTIN, OUTPUT);

  //PWM
  pinMode(IN1, OUTPUT);     // Initialize the Motor PWM pin as an output
  pinMode(IN2, OUTPUT);
  
  pinMode(IN3, OUTPUT);     // Initialize the Motor PWM pin as an output
  pinMode(IN4, OUTPUT);
  
  analogWriteFrequency(200);  

  analogWrite(IN1, 0);
  analogWrite(IN2, 0);
  analogWrite(IN3, 0);
  analogWrite(IN4, 0);

  // Serial Begin
  Serial.begin(115200);
  Serial.println("Booting");


  // WIFI Server AP (This Create a WIFI Server in ESP32 with IP: 192.168.4.1  by default) 
  WiFi.softAP(ssid, password);             // Start the access point
  Serial.print("Access Point \"");
  Serial.print(ssid);
  Serial.println("\" started");

  Serial.print("IP address:\t");
  Serial.println(WiFi.softAPIP());         // Send the IP address of the ESP32 to the computer

  server.on("/", handleRoot);  
  server.on("/motor", HTTP_GET, motor);
  server.on("/para", HTTP_GET, halt);
  
  server.on("/sonar1", HTTP_GET, sonar1);
  server.on("/sonar2", HTTP_GET, sonar2);

  server.onNotFound(handleNotFound);

  server.on("/ligaled", []() {
    server.send(200, "text/plain", "ligou");
    digitalWrite(LED_BUILTIN, HIGH);
  });

  server.on("/desligaled", []() {
    server.send(200, "text/plain", "apagou");
    digitalWrite(LED_BUILTIN, LOW);
  });

  server.begin();
  Serial.println("HTTP server started");
  //END OF WIFI Server AP


  ArduinoOTA.onStart([]() {
    String type;
    if (ArduinoOTA.getCommand() == U_FLASH) {
      type = "sketch";
    } else { 
      type = "filesystem";
    }

    Serial.println("Start updating " + type);
  });
  ArduinoOTA.onEnd([]() {
    Serial.println("\nEnd");
  });
  ArduinoOTA.onProgress([](unsigned int progress, unsigned int total) {
    Serial.printf("Progress: %u%%\r", (progress / (total / 100)));
  });
  ArduinoOTA.onError([](ota_error_t error) {
    Serial.printf("Error[%u]: ", error);
    if (error == OTA_AUTH_ERROR) {
      Serial.println("Auth Failed");
    } else if (error == OTA_BEGIN_ERROR) {
      Serial.println("Begin Failed");
    } else if (error == OTA_CONNECT_ERROR) {
      Serial.println("Connect Failed");
    } else if (error == OTA_RECEIVE_ERROR) {
      Serial.println("Receive Failed");
    } else if (error == OTA_END_ERROR) {
      Serial.println("End Failed");
    }
  });
  ArduinoOTA.begin();
  Serial.println("Ready");

  // END OF OTA SETUP
}

void LeSensor(){
  sonar1();
  sonar2();
}

void IA(){
  if(s1 == 0 && s2 == 1) comando = direita;
  else if (s1 == 1 && s2 == 0) comando = esquerda;
  else if (s1 == 1 && s2 == 1) comando = esquerda;
  else comando = frente;
}

void Motor(){
  switch(comando){
    case direita: 
      analogWrite(IN1, 0);
      analogWrite(IN2, 0);
      analogWrite(IN3, speed2);
      analogWrite(IN4, 0);
      break;

    case esquerda: 
      analogWrite(IN1, speed1);
      analogWrite(IN2, 0);
      analogWrite(IN3, 0);
      analogWrite(IN4, 0);
      break;
    
    case frente: 
      analogWrite(IN1, speed1);
      analogWrite(IN2, 0);
      analogWrite(IN3, speed2);
      analogWrite(IN4, 0);
      break;

   case para: 
      analogWrite(IN1, 0);
      analogWrite(IN2, 0);
      analogWrite(IN3, 0);
      analogWrite(IN4, 0);
      break;  
  }
}

void loop() {
  ArduinoOTA.handle();

  server.handleClient();

/*  if (parado == 0){
    LeSensor();
    IA();
    Motor();
  }
*/
  count++;
  if(count > 100)
    halt();
    
  delay(10);
}
