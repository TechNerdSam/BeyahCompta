# 💰 BéyahCompta - Gestion Financière Épurée 💸

## Table des Matières
1.  [🌟 Introduction](#-introduction)
2.  [✨ Fonctionnalités Clés](#-fonctionnalités-clés)
3.  [💻 Technologies Utilisées](#-technologies-utilisées)
4.  [🚀 Démarrage Rapide](#-démarrage-rapide)
    * [Prérequis](#prérequis)
    * [Compilation](#compilation)
    * [Exécution](#exécution)
5.  [📂 Structure du Projet](#-structure-du-projet)
6.  [🛠️ Utilisation de l'Application](#️-utilisation-de-lapplication)
    * [Gestion des Transactions](#gestion-des-transactions)
    * [Rapports Financiers](#rapports-financiers)
    * [Gestion Budgétaire](#gestion-budgétaire)
7.  [⚙️ Personnalisation (Développeurs)](#️-personnalisation-développeurs)
8.  [🤝 Contribution](#-contribution)
9.  [📞 Contact](#-contact)

## 🌟 Introduction

BéyahCompta est une application de gestion financière intuitive et épurée, conçue pour vous aider à suivre vos transactions, gérer vos soldes de compte et maîtriser votre budget avec une efficacité remarquable. Oubliez la complexité des feuilles de calcul et des logiciels surchargés. BéyahCompta offre une interface utilisateur propre et conviviale, se concentrant sur l'essentiel pour une expérience de gestion financière agréable et sans tracas. 🧘‍♀️📊

Que vous soyez un particulier souhaitant suivre ses dépenses quotidiennes ou une petite entreprise gérant ses flux de trésorerie, BéyahCompta est l'outil qu'il vous faut pour une visibilité claire de votre santé financière. 💖

## ✨ Fonctionnalités Clés

* **Ajout, Modification et Suppression de Transactions** : Gérez facilement vos débits et crédits avec des champs clairs pour le compte, le type, la catégorie, la description et le montant. ➕✏️🗑️
* **Filtrage et Recherche Intelligents** : Trouvez rapidement des transactions spécifiques par type, catégorie ou texte dans la description ou le compte. 🕵️‍♀️🔍
* **Synthèse des Soldes de Compte** : Visualisez instantanément les soldes de vos comptes (`Caisse`, `Banque`, `Épargne` par défaut) et un solde global agrégé. 🏦
* **Rapports Financiers Détaillés** : Obtenez un aperçu clair de vos totaux de crédits et débits, ainsi qu'un résumé budgétaire par catégorie. 📈
* **Visualisation des Dépenses par Catégorie** : Un graphique circulaire interactif (Pie Chart) vous montre où va votre argent, facilitant l'identification des domaines de dépenses majeurs. 🥧
* **Gestion des Budgets par Catégorie** : Définissez et suivez des budgets pour chaque catégorie de dépenses, avec un indicateur visuel lorsque les limites sont dépassées. 🎯
* **Exportation CSV** : Exportez toutes vos transactions vers un fichier CSV pour une analyse plus approfondie ou pour l'intégration avec d'autres outils. 📥
* **Persistance des Données** : Toutes vos données sont automatiquement sauvegardées et chargées au démarrage, assurant qu'aucune information n'est perdue. 💾
* **Expérience Utilisateur Agréable** : Basé sur Swing, avec un design inspiré de Material Design et des effets de survol pour une interaction fluide. 🎨

## 💻 Technologies Utilisées

* **Java (JDK 8+)** : Langage de programmation principal. ☕
* **Swing** : Toolkit graphique pour le développement de l'interface utilisateur. 🖼️
* **Sérialisation d'Objets Java** : Pour la persistance des données. 🔄
* **`java.time` (API Date & Time)** : Pour la gestion moderne des dates. 🗓️
* **`NumberFormat`** : Pour un formatage monétaire et numérique localisé (français). 💶

## 🚀 Démarrage Rapide

Suivez ces étapes pour compiler et exécuter BéyahCompta sur votre machine locale.

### Prérequis
Assurez-vous d'avoir le **Java Development Kit (JDK) version 8 ou supérieure** installé sur votre système. Vous pouvez le télécharger depuis le site officiel d'Oracle ou utiliser une distribution OpenJDK.

### Compilation
1.  **Clonez le dépôt** (ou téléchargez le fichier `BeyahCompta.java`) :
    ```bash
    git clone [https://github.com/technerdsam/BeyahCompta.git](https://github.com/votre_utilisateur/BeyahCompta.git)
    cd BeyahCompta/BéyahCompta # Assurez-vous d'être dans le bon répertoire
    ```
    *(Remarque: Ajustez le chemin si votre structure de dossiers est différente.)*

2.  **Compilez les fichiers source** :
    Ouvrez un terminal ou une invite de commande dans le répertoire où se trouvent `BeyahCompta.java`, `Transaction.java`, `TransactionType.java`, `TransactionCategory.java` et `PieChartPanel.java` (si elles sont séparées).
    ```bash
    javac BeyahCompta.java
    ```
    *(Le compilateur Java détectera et compilera automatiquement les classes internes et les enums si elles sont correctement définies au sein de `BeyahCompta.java` ou dans des fichiers séparés dans le même répertoire.)*

### Exécution
Après la compilation réussie, exécutez l'application :

```bash
java BeyahCompta
