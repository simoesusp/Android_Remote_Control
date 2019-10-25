
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
 * Modulo PonteH Monster:   INA1,INB1(Motor1)     INA2,INB2(Motor2)
 *          Horario           1    0                1   0
 *          Anti-Hor          0    1                0   1
 *          Break to Ground   0    0                0   0
 *          Break to VCC      1    1                1   1
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

#define m1PWM 23
#define m2PWM 22
#define INA1 21   //Motor1, entrada A
#define INA2 19   //Motor2, entrada A
#define INB1 18   //Motor1, entrada B
#define INB2 5    //Motor2, entrada B


int dir1 = 0;
int dir2 = 0;
int speed1 = 0;
int speed2 = 0;
int count = 0;  //Para se perdero contatocom cel

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

    if(dir1 == 0) //Pra Frente
    {   digitalWrite(INA1, 1);
        digitalWrite(INB1, 0);
    }
    else          //Pra Tras
    {   digitalWrite(INA1, 0);
        digitalWrite(INB1, 1);
    }
    
    if(dir2 == 0)
    { 
      digitalWrite(INA2, 1);
      digitalWrite(INB2, 0);
    } else 
    {
      digitalWrite(INA2, 0);
      digitalWrite(INB2, 1);
    }

    analogWrite(m1PWM,speed1);
    analogWrite(m2PWM,speed2);
         
    count = 0;   // Reseta Timer
    server.send(200, "text/plain", "");   // NAO apague isso, pois o controle do Natan esperapela resposta!!
   
  }
}

void halt() {
    digitalWrite(INA1, 0);  // Break to Ground
    digitalWrite(INA2, 0);
    digitalWrite(INB1, 0);
    digitalWrite(INB2, 0);

    analogWrite(m1PWM, 0);
    analogWrite(m2PWM, 0);

    speed1 = 0;
    speed2 = 0;
  
    count = 0;
    server.send(200, "text/plain", "");   // NAO apague isso, pois o controle do Natan esperapela resposta!!
}


void setup() {  
  //LED
  pinMode(LED_BUILTIN, OUTPUT);

  //PWM
  pinMode(m1PWM, OUTPUT);     // Initialize the Motor PWM pin as an output
  pinMode(m2PWM, OUTPUT);
  pinMode(INA1, OUTPUT);
  pinMode(INB1, OUTPUT);
  pinMode(INA2, OUTPUT);
  pinMode(INB2, OUTPUT);
  
  analogWriteFrequency(200);  

  digitalWrite(INA1, 0); // Frente
  digitalWrite(INB1, 1);
  digitalWrite(INA2, 0);  // Frente
  digitalWrite(INB2, 1);

  analogWrite(m1PWM, 0);  // Sepeed = 0
  analogWrite(m2PWM, 0);

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

void loop() {
  ArduinoOTA.handle();

  server.handleClient();

  count++;
  if(count > 100) // conta 1 seg no delay(10) ... Esta' bem mais rapido!!! Nao sei por que!!
    halt();
    
  delay(10);    // Botei Delay pra economizar bateria do WIFI
}
