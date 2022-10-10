# DevMob RGB Weather Kit

## Soft utilisé sur le Raspberry PI:
- Librairies utilisées:
    - https://github.com/petzval/btferret Pour le Bluetooth Low Energy
    - https://learn.adafruit.com/adafruit-rgb-matrix-bonnet-for-raspberry-pi/driving-matrices Pour la gestion de la matrice de LEDs

## Méthode pour build
1. Installer `libbluetooth-dev`, `libreadline-dev` et `git`
    <details><summary>Commande sur Ubuntu/Debian/Raspberry PI OS</summary>   
    
        sudo apt update && sudo apt install libbluetooth-dev libreadline-dev git
    
    </details>
2. Récupérer le git
`git clone https://gitlab-etu.ing.he-arc.ch/isc/2022-23/niveau-3/3294-4-devemob/devmob-rgbweatherkit.git`
3. Lancer la commande `make`
4. Lancer le logiciel avec `sudo ./main` 
(Il est important de lancer l'application avec les droits d'administrateurs au risque que le Bluetooth ne fonctionne pas).
