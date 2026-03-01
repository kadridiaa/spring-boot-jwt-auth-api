# Installation et Configuration MySQL

## ✅ Étapes après l'installation de MySQL

### 1. Vérifier que MySQL fonctionne

```powershell
mysql --version
```

### 2. Se connecter à MySQL

```powershell
mysql -u root -p
```
Entrez le mot de passe root que vous avez défini lors de l'installation.

### 3. Créer la base de données

Deux options :

**Option A : Depuis MySQL CLI**
```sql
CREATE DATABASE IF NOT EXISTS authdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit;
```

**Option B : Avec le script SQL**
```powershell
mysql -u root -p < database\mysql_setup.sql
```

### 4. Vérifier la base de données

```powershell
mysql -u root -p
```

```sql
SHOW DATABASES;
USE authdb;
SHOW TABLES;
exit;
```

### 5. Ajuster le mot de passe dans application.properties

Si votre mot de passe root n'est pas `root123`, modifiez dans `src/main/resources/application.properties` :

```properties
spring.datasource.password=VOTRE_MOT_DE_PASSE
```

### 6. Démarrer l'application

```powershell
mvn clean install
mvn spring-boot:run
```

## 🔧 Configuration actuelle

- **Base de données** : authdb
- **Utilisateur** : root
- **Mot de passe** : root123 (à modifier si différent)
- **Port** : 3306
- **URL** : jdbc:mysql://localhost:3306/authdb

## 📊 Données de test

L'application créera automatiquement au démarrage :
- **Utilisateur** : user / password123
- **Admin** : admin / admin123

Les tables seront créées automatiquement par Spring (hibernate ddl-auto=update).

## ⚠️ Problèmes courants

### Erreur de connexion
- Vérifiez que MySQL est démarré : 
  ```powershell
  Get-Service MySQL*
  ```
- Si arrêté : 
  ```powershell
  Start-Service MySQL80
  ```

### Mot de passe incorrect
Modifiez `application.properties` avec le bon mot de passe.

### Port occupé
Vérifiez le port 3306 dans MySQL Workbench ou modifiez le port dans `application.properties`.
