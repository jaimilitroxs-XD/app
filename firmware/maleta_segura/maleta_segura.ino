// Maleta segura - ESP32 con alarma y BLE

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLESecurity.h>
#include <BLE2902.h>
#include "secretos.h"

#define NOMBRE_BLE         "MaletaSegura"
#define UUID_SERVICIO      "6e1a0001-4b5d-4c3a-9f2e-6d616c657461"
#define UUID_ESTADO        "6e1a0002-4b5d-4c3a-9f2e-6d616c657461"
#define UUID_COMANDO       "6e1a0003-4b5d-4c3a-9f2e-6d616c657461"

const int PIN_REED      = 27;  // LOW = cerrada
const int PIN_VIBRACION = 14;  // HIGH = vibración (SW-420)
const int PIN_BUZZER    = 25;  // buzzer activo
const int PIN_LED       = 26;

// Ajustes de la alarma
const unsigned long GRACIA_ARMADO_MS   = 3000;
const unsigned long REBOTE_REED_MS     = 50;
const int           PULSOS_VIBRACION   = 3;
const unsigned long VENTANA_VIB_MS     = 1000;
const unsigned long PARPADEO_ALARMA_MS = 150;
const unsigned long PARPADEO_ARMADA_MS = 2000;
const size_t        LARGO_MAX_COMANDO  = 32;

enum Estado { DESARMADA, ARMADA, ALARMA };
Estado estado = DESARMADA;

unsigned long momentoArmado = 0;
bool buzzerManual = false;
bool ledManual = false;
bool buzzerSilenciado = false;

int reedEstable = HIGH;
int reedLeido = HIGH;
unsigned long reedCambio = 0;

int vibAnterior = LOW;
int pulsosVib = 0;
unsigned long inicioVentanaVib = 0;

String bufferSerie;

// BLE
BLECharacteristic* caracEstado = nullptr;
const int MAX_CELULARES = 2;  // celulares a la vez
volatile int celulares = 0;  // celulares conectados ahora
volatile bool cambioConexion = false;
QueueHandle_t colaComandos;

struct Comando { char texto[LARGO_MAX_COMANDO + 1]; };

const char* nombreEstado(Estado e) {
  switch (e) {
    case DESARMADA: return "DESARMADA";
    case ARMADA:    return "ARMADA";
    case ALARMA:    return "ALARMA";
  }
  return "?";
}

bool maletaCerrada() { return reedEstable == LOW; }

void enviar(const String& msg) {
  Serial.println(msg);
  if (caracEstado != nullptr && celulares > 0) {
    caracEstado->setValue(msg.c_str());
    caracEstado->notify();
  }
}

void enviarEstado() {
  enviar(String("ESTADO:") + nombreEstado(estado) +
         ";MALETA:" + (maletaCerrada() ? "CERRADA" : "ABIERTA"));
}

void pitar(int veces) {
  for (int i = 0; i < veces; i++) {
    digitalWrite(PIN_BUZZER, HIGH);
    delay(80);
    digitalWrite(PIN_BUZZER, LOW);
    if (i < veces - 1) delay(120);
  }
}

void cambiarEstado(Estado nuevo) {
  estado = nuevo;
  buzzerSilenciado = false;
  enviarEstado();
}

void dispararAlarma(const char* causa) {
  if (estado != ARMADA) return;
  if (millis() - momentoArmado < GRACIA_ARMADO_MS) return;
  enviar(String("ALERTA:") + causa);
  cambiarEstado(ALARMA);
}

void procesarComando(String cmd) {
  cmd.trim();
  cmd.toUpperCase();
  if (cmd.length() == 0) return;

  if (cmd == "ARMAR") {
    if (estado != DESARMADA) { enviar("ERROR:YA_ARMADA"); return; }
    if (!maletaCerrada())    { enviar("ERROR:MALETA_ABIERTA"); return; }
    momentoArmado = millis();
    pulsosVib = 0;
    enviar("OK:ARMAR");
    pitar(1);
    cambiarEstado(ARMADA);
  } else if (cmd == "DESARMAR") {
    enviar("OK:DESARMAR");
    pitar(2);
    cambiarEstado(DESARMADA);
  } else if (cmd == "BUZZER_ON") {
    buzzerManual = true;
    buzzerSilenciado = false;
    enviar("OK:BUZZER_ON");
  } else if (cmd == "BUZZER_OFF") {
    buzzerManual = false;
    if (estado == ALARMA) buzzerSilenciado = true;
    enviar("OK:BUZZER_OFF");
  } else if (cmd == "LED_ON") {
    ledManual = true;
    enviar("OK:LED_ON");
  } else if (cmd == "LED_OFF") {
    ledManual = false;
    enviar("OK:LED_OFF");
  } else if (cmd == "ESTADO") {
    enviarEstado();
  } else {
    enviar("ERROR:COMANDO_DESCONOCIDO");
  }
}

void leerReed() {
  int v = digitalRead(PIN_REED);
  if (v != reedLeido) {
    reedLeido = v;
    reedCambio = millis();
  }
  if (reedLeido != reedEstable && millis() - reedCambio >= REBOTE_REED_MS) {
    reedEstable = reedLeido;
    enviarEstado();
    if (!maletaCerrada()) dispararAlarma("APERTURA");
  }
}

void leerVibracion() {
  int v = digitalRead(PIN_VIBRACION);
  if (v == HIGH && vibAnterior == LOW) {
    unsigned long ahora = millis();
    if (pulsosVib == 0 || ahora - inicioVentanaVib > VENTANA_VIB_MS) {
      pulsosVib = 0;
      inicioVentanaVib = ahora;
    }
    pulsosVib++;
    if (pulsosVib >= PULSOS_VIBRACION) {
      pulsosVib = 0;
      dispararAlarma("VIBRACION");
    }
  }
  vibAnterior = v;
}

void actualizarActuadores() {
  unsigned long ahora = millis();
  bool buzzer = buzzerManual;
  bool led = ledManual;

  if (estado == ALARMA) {
    bool fase = (ahora / PARPADEO_ALARMA_MS) % 2;
    led = led || fase;
    if (!buzzerSilenciado) buzzer = buzzer || fase;
  } else if (estado == ARMADA) {
    led = led || (ahora % PARPADEO_ARMADA_MS < 60);
  }

  digitalWrite(PIN_BUZZER, buzzer ? HIGH : LOW);
  digitalWrite(PIN_LED, led ? HIGH : LOW);
}

void leerSerie() {
  while (Serial.available()) {
    char c = Serial.read();
    if (c == '\n' || c == '\r') {
      procesarComando(bufferSerie);
      bufferSerie = "";
    } else if (bufferSerie.length() < LARGO_MAX_COMANDO) {
      bufferSerie += c;
    }
  }
}

class ServidorCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* servidor) override {
    celulares++;
    cambioConexion = true;
    if (celulares < MAX_CELULARES) BLEDevice::startAdvertising();  // espacio para otro
  }
  void onDisconnect(BLEServer* servidor) override {
    if (celulares > 0) celulares--;
    cambioConexion = true;
  }
};

class ComandoCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* carac) override {
    String valor = carac->getValue();
    if (valor.length() == 0 || valor.length() > LARGO_MAX_COMANDO) return;  // descarta mensajes inválidos
    Comando c;
    strncpy(c.texto, valor.c_str(), LARGO_MAX_COMANDO);
    c.texto[LARGO_MAX_COMANDO] = '\0';
    xQueueSend(colaComandos, &c, 0);
  }
};

class SeguridadCallbacks : public BLESecurityCallbacks {
  void onAuthenticationComplete(esp_ble_auth_cmpl_t desc) override {
    if (desc.success) {
      Serial.println("INFO:EMPAREJADO_OK");
    } else {
      Serial.printf("INFO:EMPAREJAMIENTO_FALLIDO;MOTIVO:0x%02X\n", desc.fail_reason);
    }
  }
};

void iniciarBLE() {
  colaComandos = xQueueCreate(8, sizeof(Comando));

  BLEDevice::init(NOMBRE_BLE);

  BLESecurity* seguridad = new BLESecurity();
  seguridad->setPassKey(true, PIN_BLE);
  seguridad->setCapability(ESP_IO_CAP_OUT);
  seguridad->setAuthenticationMode(true, true, false);
  BLEDevice::setSecurityCallbacks(new SeguridadCallbacks());

  BLEServer* servidor = BLEDevice::createServer();
  servidor->setCallbacks(new ServidorCallbacks());
  servidor->advertiseOnDisconnect(true);

  BLEService* servicio = servidor->createService(UUID_SERVICIO);

  caracEstado = servicio->createCharacteristic(
      UUID_ESTADO,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY |
      BLECharacteristic::PROPERTY_READ_AUTHEN);
  caracEstado->setAccessPermissions(ESP_GATT_PERM_READ_ENC_MITM);
  BLE2902* cccd = new BLE2902();
  cccd->setAccessPermissions(ESP_GATT_PERM_READ_ENC_MITM | ESP_GATT_PERM_WRITE_ENC_MITM);
  caracEstado->addDescriptor(cccd);

  BLECharacteristic* caracComando = servicio->createCharacteristic(
      UUID_COMANDO,
      BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_AUTHEN);
  caracComando->setAccessPermissions(ESP_GATT_PERM_WRITE_ENC_MITM);
  caracComando->setCallbacks(new ComandoCallbacks());

  servicio->start();

  BLEAdvertising* anuncio = BLEDevice::getAdvertising();
  anuncio->addServiceUUID(UUID_SERVICIO);
  anuncio->setScanResponse(true);
  BLEDevice::startAdvertising();
}

void atenderBLE() {
  if (cambioConexion) {
    cambioConexion = false;
    Serial.printf("INFO:CELULARES_CONECTADOS:%d\n", (int)celulares);
  }
  Comando c;
  while (xQueueReceive(colaComandos, &c, 0) == pdTRUE) {
    procesarComando(String(c.texto));
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_REED, INPUT_PULLUP);
  pinMode(PIN_VIBRACION, INPUT_PULLDOWN);
  pinMode(PIN_BUZZER, OUTPUT);
  pinMode(PIN_LED, OUTPUT);

  delay(300);
  reedEstable = reedLeido = digitalRead(PIN_REED);
  vibAnterior = digitalRead(PIN_VIBRACION);

  iniciarBLE();

  Serial.println();
  Serial.println("=== Maleta segura - alarma + BLE (v3: hasta 2 celulares) ===");
  Serial.println("Bluetooth: " NOMBRE_BLE " (se pide PIN al emparejar)");
  Serial.println("Comandos: ARMAR, DESARMAR, BUZZER_ON, BUZZER_OFF, LED_ON, LED_OFF, ESTADO");
  enviarEstado();
}

void loop() {
  leerSerie();
  atenderBLE();
  leerReed();
  leerVibracion();
  actualizarActuadores();
}
