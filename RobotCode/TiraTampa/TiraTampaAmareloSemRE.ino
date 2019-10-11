
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
 */


#define LED_BUILTIN 2
#include <ArduinoOTA.h>   // Library to allow programing via WIFI (Choose ip port in Arduino IDE
#include <WiFi.h>        // Include the Wi-Fi library
#include <WebServer.h>
#include <analogWrite.h>  // Aparently, you can analogRead, but not analogWrite without a library with ESP32 !!!

#define D1 23
#define D2 22
#define D3 21
#define D4 19


#define motor1PWM D1
#define motor2PWM D2
#define motor1Dir D3
#define motor2Dir D4  // Nao sei se isso acontece no ESP32!!! Nao da' pra usar o D4, pois quando reprograma o NODE omotor comeca a andar

int dir1 = 0;
int dir2 = 0;
int count = 0;

const char *ssid = "ESP32 Access Point"; // The name of the Wi-Fi network that will be created
const char *password = "your-password";   // The password required to connect to it, leave blank for an open

WebServer server(80);


void handleRoot() {
  server.send(200, "text/plain", "Hello from ESP32 !");
}

void handleNotFound() {
  String message = "File Not Found\n\n";
  message += "URI: ";
  message += server.uri();
  message += "\nMethod: ";
  message += (server.method() == HTTP_GET) ? "GET" : "POST";
  message += "\nArguments: ";
  message += server.args();
  message += "\n";
  for (uint8_t i = 0; i < server.args(); i++) {
    message += " " + server.argName(i) + ": " + server.arg(i) + "\n";
  }
  server.send(404, "text/plain", message);
}

void motor() {
  if(server.hasArg("speed1")) {
    int speed1 = server.arg("speed1").toInt();
    int speed2 = server.arg("speed2").toInt();

    dir1 = server.arg("dir1").toInt();
    dir2 = server.arg("dir2").toInt();

    Serial.println("Motor 1:");
    Serial.println(speed1);
    Serial.println(dir1);

    Serial.println("Motor 2:");
    Serial.println(speed2);
    Serial.println(dir2);


    if(dir1 == 0)
      analogWrite(motor1PWM, speed1);
    else
      analogWrite(motor1PWM, 1024-speed1);

    if(dir2 == 0)
      analogWrite(motor2PWM, speed2);
    else
      analogWrite(motor2PWM, 1024-speed2);

    digitalWrite(motor1Dir, dir1);
    digitalWrite(motor2Dir, dir2);
    
    server.send(200, "text/html", "Ok!");
    count = 0;
  }
}

void para() {
    if(dir1 == 0)
      analogWrite(motor1PWM, 0);
    else
      analogWrite(motor1PWM, 1024);

    if(dir2 == 0)
      analogWrite(motor2PWM, 0);
    else
      analogWrite(motor2PWM, 1024);

    count = 0;

    Serial.println("Motor 1:");
    Serial.println(0);
    Serial.println(dir1);

    Serial.println("Motor 2:");
    Serial.println(0);
    Serial.println(dir2);
    server.send(200, "text/html", "Ok Parou!!!");
    Serial.println("OK Parou!!!");
    
}

void setup() {
  //LED
  pinMode(LED_BUILTIN, OUTPUT);

  //PWM
  pinMode(motor1PWM, OUTPUT);     // Initialize the Motor PWM pin as an output
  pinMode(motor2PWM, OUTPUT);
  
  pinMode(motor1Dir, OUTPUT);     // Initialize the Motor PWM pin as an output
  pinMode(motor2Dir, OUTPUT);
  
  analogWriteFrequency(200);  

  analogWrite(motor1PWM, 0);
  analogWrite(motor2PWM, 0);

  digitalWrite(motor1Dir, dir1);
  digitalWrite(motor2Dir, dir2);

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
  server.on("/para", HTTP_GET, para);

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


//  
//  // OTA SETUP
//  WiFi.mode(WIFI_STA);
//  WiFi.begin(ssid, password);
//  while (WiFi.waitForConnectResult() != WL_CONNECTED) {
//    Serial.println("Connection Failed! Rebooting...");
//    delay(500);
//    ESP.restart();
//  }

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
//  Serial.print("IP address: ");
//  Serial.println(WiFi.localIP());

  // END OF OTA SETUP
}

void loop() {
  ArduinoOTA.handle();

  server.handleClient();

  count++;
  if(count > 100)
    para();
    
  delay(10);
}
