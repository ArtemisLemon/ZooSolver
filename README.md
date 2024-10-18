# Génération de CSP à partir d'un fichier xcore

Ce petit projet montre comment configurer gradle pour générer les sources java correspondantes à un fichier xcore.
Et comment lier ce model a un CSP.

Il y a aussi des contraintes OCL modelisees' en composant les sub-CSP comme navCSP (NavigationOrAttributeCall)

Based on`git clone https://git.kher.nl/cours/base-xtext.git`

## Interesting Files
- model/zoo.xcore (meta-model)
    - myZoo.xmi (model instance to solve for)
    - myZooConfig.xmi (solved model instance)
- src/main/java/mde/ZooBuilder (generate instances .xmi)
- src/main/java/mde/App.java (translation to Choco solver)
    - MVIPropagator (additional propagator)

## Compilation & exécution

```bash
# build
./gradlew build

# run
./gradlew run
```
