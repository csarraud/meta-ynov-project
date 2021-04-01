# Projet OS Embarqué : Distribution YOCTO

L'objectif de ce projet est de générer une distribution Yocto et de créer un service qui exécute un code en C au démarrage de la carte. Notre objectif est d'afficher la température lue par le capteur de température de la Warp 7 à chaque démarrage de celle-ci, et d'allumer les LEDS correspondantes à la température reçue. 

# Sommaire
#### I)	Créer notre propre distribution Linux 
#### II)	Programme en C qui affiche la température au démarrage de la carte
#### III)	Lancer au démarrage de la carte Warp7 le service grace à un repo GitHub 
 
_____________________________________

## I)	Créer notre propre distribution Linux

### 1. Création de la layer
    bitbake-layers create-layer../meta-ynov-project
    bitbake-layers add-layer ../meta-ynov-project

### 2. Modification de la layer
    code ../meta-ynov-project

Nous avons modifié tous les fichiers nécessaires à la création de la distribution afin d'obtenir l'arborescence suivante : 

        ../meta-ynov-project
    ├── conf
    │   ├── distro
    │   │   └── ynov-project.conf
    │   └── layer.conf
    ├── COPYING.MIT
    ├── README
    ├── recipes-bsp
    │   └── u-boot
    │       ├── u-boot-fslc_%.bbappend
    │       ├── u-boot-scr
    │       │   └── boot.cmd
    │       └── u-boot-scr.bb
    ├── recipes-core
    │   ├── images
    │   │   └── ynov-image-project.bb
    │   └── systemd
    │       ├── systemd-conf
    │       │   ├── eth0.network
    │       │   └── usb0.network
    │       └── systemd-conf_%.bbappend
    ├── recipes-kernel
    │   └── linux
    │       ├── linux-fslc
    │       │   ├── 0001-add-leds-in-device-tree.patch
    │       │   └── fragment.cfg
    │       └── linux-fslc_%.bbappend
    └── recipes-ynov
        └── temperature
            ├── temperature
            │   └── temperature.service
            └── temperature.bb

Les fichiers de *meta-ynov-project* sont présents dans leur intégralité dans le repository pour de plus amples vérifications.


## II)	Programme en C qui affiche la température et les LEDS correspondantes

| Address register | Name | Description |
|------------------|------|-------------|   
04h	|  OUT_T_MSB | Temperature data | Integer part in °C 
05h	|  OUT_T_LSB | Temperature data | Fractional part in °C

### 1. Par le Kernel :
Pour obtenir la température, puis le scale envoyée par le capteur, nous avons fait : 

    cat /sys/bus/iio\devices/iio\:device0/in_temp_raw  
    cat /sys/bus/iio\devices/iio\:device0/in_temp_scale  

Pour obtenir la température actuelle nous avons fait la multiplication de ces deux valeurs reçues. 

*Aucune LED allumée -> 27°C   
Toutes les LEDS allumées -> 35°C  
1 LED allumée -> +1°C*

### 2. Application et code en C : temperature.c

    #include <stdio.h>
    #include <stdint.h>
    #include <stdlib.h>

    #define PATH_LED_0 "/sys/class/leds/d0/brightness"
    #define PATH_LED_1 "/sys/class/leds/d1/brightness"
    #define PATH_LED_2 "/sys/class/leds/d2/brightness"
    #define PATH_LED_3 "/sys/class/leds/d3/brightness"
    #define PATH_LED_4 "/sys/class/leds/d4/brightness"
    #define PATH_LED_5 "/sys/class/leds/d5/brightness"
    #define PATH_LED_6 "/sys/class/leds/d6/brightness"
    #define PATH_LED_7 "/sys/class/leds/d7/brightness"

    #define ON "255"
    #define OFF "0"

    #define PATH_TEMP_RAW "/sys/bus/iio/devices/iio:device0/in_temp_raw"
    #define PATH_TEMP_SCALE "/sys/bus/iio/devices/iio:device0/in_temp_scale"

    char* paths[8] = {PATH_LED_0, PATH_LED_1, PATH_LED_2, PATH_LED_3, PATH_LED_4, PATH_LED_5, PATH_LED_6, PATH_LED_7};

    void set_led(char* path, char* value) {
        FILE* file = NULL;

        file = fopen(path, "w+");
        
        if(file != NULL) {
            fputs(value, file);
            fclose(file);
        }
        else {
            printf("[set_leds] Cannot open file %s\n", path);
        }
    }

    uint32_t get_temperature_raw() {
        FILE* file = NULL;

        int buffer_length = 255;
        char buffer[buffer_length];
        
        file = fopen(PATH_TEMP_RAW, "r+");
        
        if(file != NULL) {
            fgets(buffer, buffer_length, file);
            fclose(file);
            uint32_t raw = (uint32_t)atoi(buffer);
            return raw;
        }
        else {
            printf("[get_temperature_raw] Cannot open file %s\n", PATH_TEMP_RAW);
        }

        return 0;
    }

    float get_temperature_scale() {
        FILE* file = NULL;

        int buffer_length = 255;
        char buffer[buffer_length];
        
        file = fopen(PATH_TEMP_SCALE, "r+");
        
        if(file != NULL) {
            fgets(buffer, buffer_length, file);
            fclose(file);
            float raw = (float)atof(buffer);
            return raw;
        }
        else {
            printf("[get_temperature_scale] Cannot open file %s\n", PATH_TEMP_SCALE);
        }

        return 0.0f;
    }

    void set_leds_from_temperature(float temperature) {
        uint8_t integer_temperature = (int) temperature;

        uint8_t nb_leds = (int) (integer_temperature - 28) / 2;

        for(int i=0; i<nb_leds; i++) {
            set_led(paths[i], ON);
        }

        for(int i=nb_leds; i<(int) (sizeof(paths)/sizeof(paths[0])); i++) {
            set_led(paths[i], OFF);
        }
    }

    void do_temperature_scan() {

        while(1) {
            uint32_t temperature_raw = get_temperature_raw();
            float temperature_scale = get_temperature_scale();

            float temperature = temperature_raw * temperature_scale;
            set_leds_from_temperature(temperature);
            
            usleep(200000);
        }
    }

    int main(int argc, char *argv[]) {

        do_temperature_scan();

        return 0;
    }


## III)	Lancer au démarrage de la carte Warp7 le service grace à un repo GitHub 

Pour lancer au démarrage de la carte le service qui nous affichera la température et nous allumera les LEDS en fonction de la température reçue, nous avons :

### 1. Crée le fichier temperature.service

    [Unit]
    Description=Temperature display with LED

    [Service]
    Type=simple
    ExecStart=/usr/bin/temperature
    Restart=on-failure
    RestartSec=10

    [Install]
    WantedBy=multi-user.target

### 2. Modifié le CMakeLists.txt

    cmake_minimum_required(VERSION 3.10)
    project (temperature)

    # Compile the C code to executable
    add_executable(temperature temperature.c)

    # Install executable to /usr/bin
    install(TARGETS temperature RUNTIME DESTINATION /usr/bin)

Dans *devtool edit-recipe temperature*, nous avons modifié les variables suivantes : 
- SYSTEMD_SERVICE_${PN} = "temperature.service"
- SYSTEMD_AUTO_ENABLE = "enable"

### 3.  Implémentation et utilisation du repository GitHub

Lien du repository : https://github.com/csarraud/temperature

Arborescence créee : 
    
    ├── CMakeLists.txt
    ├── LICENSE
    ├── README.md
    └── src
        └── temperature.c


### 4. Compilation et déploiement sur la cible 

Dans *local.conf*, nous avons modifié la variable : 

    DISTRO ?= "ynov-project"

Puis nous avons compilé : 

    bitbake ynov-image-project

Pour le déploiement sur la cible, celle-ci doit être branchée. Ensuite, dans le U-Boot, faire la commande : 
    
    ums 0 mmc 0 

Sur le pc, faire : 

    sudo umount /dev/sdb /dev/sdb1 /dev/sdb2
    cd tmp/deploy/images/imx7s-warp
    sudo bmaptool copy ynov-image-project-imx7s-warp.wic.gz /dev/sdb

    # Attendre la fin de la compilation

Sur le U-Boot :

    reboot
