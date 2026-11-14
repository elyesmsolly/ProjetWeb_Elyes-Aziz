# Dourek : faites la queue depuis chez vous

Plateforme de files d'attente virtuelles pour les petits commerces (barbiers, salons, laboratoires, agences, administrations).
Le client trouve l'établissement qui a la plus courte attente, prend son ticket à distance et suit son tour en direct.
Le gérant crée sa file, compose le formulaire que remplit le client, et l'employé appelle le suivant depuis son écran.

Projet Web, Objectif 3, SUP'COM 2026/2027 (encadrante : Dr. Maroua Nouioua).
Réalisé par **Med Elyes Msolly** et **Med Aziz Ghazouani**.

Livrable remis sous forme de ZIP. Comme le dépôt GitHub n'est pas joint, tout ce qui s'y trouve (branches, commits, répartition des tâches) est recopié dans ce README, section 6.

### Ce que demande l'Objectif 3, et où le trouver

| Exigence du sujet | Où |
|---|---|
| Site fonctionnel, 3 à 5 pages dynamiques au moins | Section 2.1 : 12 pages, toutes alimentées par l'API |
| Front complet avec le framework choisi | Angular 22, sections 1 et 4 |
| Back minimal ou simulation d'API | API Spring Boot réelle, section 4 (routes) |
| Choix du framework | Section 1 |
| Fonctionnalités développées | Section 2 |
| Étapes de lancement | Section 3 (Windows et Linux/macOS) |
| Navigation dynamique, responsive (mobile, tablette, bureau) | Sections 2.1 et 5.4 |
| Formulaires dynamiques, filtres, notifications | Section 2 (inscription, recherche, formulaire du gérant, alertes du ticket) |
| Gestion des données et des états | API REST + base de données, signals Angular, temps réel WebSocket |
| Compatibilité Chrome, Firefox, Edge, Safari | Section 5.5 |
| Performances, accessibilité, balises sémantiques | Section 5.3 (Lighthouse) |
| Git : commits clairs, une branche par fonctionnalité, répartition des tâches | Section 6 |
| Dépendances documentées | Section 4.2 |

---

## 1. Choix du framework

| Couche | Technologie | Pourquoi |
|---|---|---|
| Front | **Angular 22** (TypeScript, composants standalone, signals) | Framework complet : routage, formulaires, HttpClient et injection de dépendances sans bibliothèque en plus. Le typage fort sécurise les échanges avec l'API. |
| Glisser-déposer | **Angular CDK** (`DragDropModule`, `moveItemInArray`, `copyArrayItem`) | Le constructeur de formulaire du gérant. Accessible et maintenu par l'équipe Angular. |
| Temps réel | **WebSocket STOMP** (`@stomp/stompjs` côté front, Spring WebSocket côté back) | La file, le ticket et l'écran employé se mettent à jour sans recharger la page. |
| Carte | **Leaflet** + OpenStreetMap | Carte des établissements et choix de l'adresse à l'inscription. |
| Back | **Spring Boot 4** (Web, Data JPA, Security, OAuth2 Resource Server, WebSocket, Validation), **Java 21** | API REST minimale mais robuste : JPA pour la base, Spring Security pour les rôles. |
| Base | **PostgreSQL** (ou H2 en mémoire avec le profil `demo`) | Relationnelle, transactions et verrous pour éviter deux tickets avec le même numéro. |
| Recherche avec l'IA | **Google Gemini** (offre gratuite, API REST) ou **API Claude** (SDK officiel `anthropic-java`) | Comprend une phrase entière (« je veux boire quelque chose »). Sans clé, une analyse locale par mots-clés prend le relais. |

Tout tourne en local, sans déploiement.

---

## 2. Fonctionnalités réalisées

### 2.1 Pages du site (toutes dynamiques)

| Page | Adresse | Accès | Contenu chargé depuis l'API |
|---|---|---|---|
| Connexion client | `/connexion` | tous (page d'arrivée) | session JWT |
| Création de compte client | `/inscription` | tous | création du compte |
| Accueil | `/` | connecté | catégories, établissements proches, vrai ticket en cours |
| Établissements (recherche, filtres, carte, IA) | `/etablissements` | connecté | liste, attente en direct, recherche avec l'IA |
| Fiche d'un établissement | `/etablissements/:slug` | connecté | prestations, statistiques, formulaire du gérant |
| Mes tickets | `/ticket` | client | tickets en cours et anciens |
| Suivi d'un ticket | `/ticket/:code` | client propriétaire | position en direct (WebSocket) |
| Connexion pro | `/pro/connexion` | tous | session gérant ou employé |
| Inscription pro en 5 étapes | `/pro/inscription` | tous | création de l'établissement, adresse par la carte |
| Tableau de bord | `/pro/tableau-de-bord` | gérant | indicateurs, file en direct |
| Formulaire client | `/pro/formulaire` | gérant | constructeur, proposition par l'IA, versions |
| Écran employé | `/pro/employe` | gérant ou employé | appel du suivant en direct |

La navigation se fait par le menu (burger sur mobile) et les liens entre pages ; les pages protégées renvoient à la bonne connexion, puis à la page demandée.

### Côté client
- **Connexion d'abord** : le site s'ouvre sur la page de connexion (`/connexion`, `/inscription` pour créer un compte).
  - Sans compte, l'accueil, les établissements et les tickets ne sont pas accessibles, et l'en-tête ne montre pas leurs onglets ; l'adresse demandée est rouverte après la connexion.
  - La session dure le temps de l'onglet (`sessionStorage`) : à chaque nouvelle ouverture du site, on repasse par la connexion. On peut donc ouvrir un compte client dans un onglet et un compte pro dans un autre.
  - Un client ne peut voir que ses propres tickets (403 sinon).
- **Accueil** : recherche par service et lieu, catégories, établissements proches avec leur attente en direct.
  - L'espace ticket montre le **vrai ticket du compte connecté**, jamais un exemple : « Vous n'avez pas de ticket en cours », ou le ticket qui passe le plus tôt (numéro, personnes avant vous, heure estimée), mis à jour en direct, suivi des autres tickets en cours.
  - Un clic sur cet espace ouvre **Mes tickets** (`/ticket`) : le ticket en cours s'il n'y en a qu'un, sinon la liste des tickets en cours (celui qui passe le plus tôt en premier), puis les anciens.
- **Recherche** (`/etablissements`) :
  - filtres par catégorie, « ouvert maintenant », attente < 20 min, < 1 km et note 4+ ;
  - tri, vue liste ou carte ;
  - temps d'attente mis à jour en direct ;
  - un seul champ et **deux boutons** : « Rechercher » (recherche normale, sans IA) et « Rechercher avec l'IA » ; le client choisit ;
  - la recherche normale devine la catégorie (« coiffure » → barbiers et coiffure femme, « barbe » → barbiers) puis garde les établissements qui correspondent le mieux aux autres mots ;
  - les fautes de frappe sont corrigées (« caffe » → café, « coifure » → coiffure) : un mot inconnu est remplacé par le mot connu le plus proche ;
  - les catégories « Autres » créées par les gérants (ex. Pressing) apparaissent comme filtres.
- **Carte** (OpenStreetMap + Leaflet, gratuit, sans clé) : bouton plein écran, touche Échap pour fermer.
- **Recherche avec l'IA** : « je veux boire quelque chose », « une coiffure à moins de 3,5 km », « le barbier le mieux noté ».
  - IA au choix : Google Gemini (gratuit) ou Claude ; sans clé, analyse locale par mots-clés.
  - L'IA lit la demande et la liste des établissements inscrits, puis renvoie en **sortie structurée** ceux qui répondent au besoin (« boire » → le café et le salon de thé) et les contraintes de la phrase : distance, attente, heure, quartier, budget, priorité.
  - **Notre code contrôle et classe** : identifiants inconnus ignorés, contraintes appliquées avec nos mesures de distance et d'attente, classement par attente, distance et avis (ou selon la priorité demandée).
  - **Liste toujours complète pour un type** : quand la demande nomme seulement un type de service (« caffe », « coiffure à moins de 3 km »), notre code ajoute les établissements de ce type que le modèle aurait oubliés (« caffe » → les deux cafés et le salon de thé). Le modèle peut aussi citer un type entier, et c'est notre code qui en énumère les établissements.
  - Tous les établissements qui correspondent sont listés ; le premier ouvert porte la marque **« Choix de l'assistant »** et l'IA explique ce choix en deux phrases.
- **Page établissement** :
  - prestations avec durée et prix, et statistiques en direct (personnes en attente, attente estimée, postes ouverts) ;
  - **formulaire dynamique** défini par le gérant ;
  - option prioritaire et heure de passage estimée.
- **Ticket en direct** (`/ticket/:code`) :
  - grand numéro façon afficheur LED et nombre de personnes avant vous ;
  - suivi étape par étape, alerte « C'est votre tour ! » avec son et vibration ;
  - « laisser passer une place », annulation en deux temps et itinéraire.

### Côté professionnel
- **Connexion** gérant ou employé (JWT) et **inscription en 5 étapes** :
  1. le compte ;
  2. l'établissement, avec sa position sur la carte ;
  3. les prestations et les postes ;
  4. le formulaire ;
  5. la confirmation.
- **Tableau de bord** du gérant :
  - indicateurs (servis, en attente, attente moyenne, taux d'absence) et affluence par heure ;
  - état des postes et file en direct ;
  - pause ou réouverture de la file.
- **Formulaire créé par l'IA** : le gérant décrit son besoin en une phrase (« Laboratoire : date de naissance, à jeun ou non, type d'ordonnance »).
  - L'IA propose les champs (sortie structurée), à l'inscription comme dans l'espace pro.
  - **Notre code contrôle** la proposition : types connus, options valides, longueurs, pas de doublon, nom et téléphone toujours en tête.
  - Le gérant garde la main : il modifie, annule la proposition ou publie. Sans clé d'IA, des règles par mots-clés prennent le relais.
- **Constructeur de formulaire** en glisser-déposer (Angular CDK) :
  - palette de 8 types de champs, réordonnancement, champ obligatoire ou non, options, duplication ;
  - modèles prêts (salon, laboratoire, agence) et aperçu en direct ;
  - publication versionnée : un client déjà dans la file garde la version qu'il a remplie.
- **Écran employé** :
  - client en cours avec ses réponses et un chronomètre ;
  - gros boutons **Appeler le suivant**, **Servi** et **Absent** ;
  - prochains clients et ajout d'un client sans smartphone.

### Règles métier (back)
- Numéros par catégorie (`B-24`), et `P-xx` pour les prioritaires.
- **1 prioritaire pour 2 clients normaux**, pour qu'aucun client n'attende indéfiniment.
- Attente estimée = personnes devant × moyenne glissante des 20 derniers services ÷ postes ouverts.
- **Verrou pessimiste** sur l'établissement lors de la prise de ticket et de l'appel : deux clients ne reçoivent jamais le même numéro.
- Après chaque changement, un message est publié sur `/topic/etablissements/{id}`. Les écrans rechargent alors l'état complet, ce qui les garde justes même après une coupure.

---

## 3. Lancer le projet

### Prérequis
- Java 21 et Maven 3.9 ou plus récent
- Node.js 22 ou plus récent (et npm)
- PostgreSQL 15 ou plus récent (facultatif grâce au profil `demo`)

### Lancement rapide sous Windows (invite de commandes cmd)
Après avoir décompressé le ZIP, ouvrir **deux** fenêtres cmd.

Fenêtre 1, le back-end (adapter le chemin de Maven s'il n'est pas dans le `Path`) :
```bat
cd chemin\vers\ProjetWeb_Elyes-Aziz-main\backend
C:\apache-maven-3.10.0\bin\mvn.cmd spring-boot:run -Dspring-boot.run.profiles=demo
```
Attendre la ligne « Started DourekApplication ». La ligne « IA de Dourek » indique si l'IA est active (API Gemini) ou en mode local.

Fenêtre 2, le front-end :
```bat
cd chemin\vers\ProjetWeb_Elyes-Aziz-main\frontend
npm install
npm start
```
Puis ouvrir http://localhost:4200 : la page de connexion s'affiche. Comptes de test : section 3.3.

Pour activer l'IA, poser le fichier `cle-ia.properties` (transmis à part, jamais dans le ZIP ni sur GitHub) dans le dossier `backend` **avant** de lancer le back-end. Sans ce fichier, tout fonctionne aussi, avec l'analyse locale par mots-clés.

### 3.1 Back-end (port 8080)

**Option A, sans rien installer (base H2 en mémoire) :**
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

**Option B, avec PostgreSQL :**
```sql
-- dans psql
CREATE USER dourek WITH PASSWORD 'dourek';
CREATE DATABASE dourek OWNER dourek;
```
```bash
cd backend
mvn spring-boot:run
```
Les tables sont créées automatiquement. Au premier lancement, des données de démonstration sont insérées : 11 établissements à l'Ariana (dont deux cafés et un salon de thé créés avec la catégorie « Autres »), des tickets et un historique.

### 3.2 Front-end (port 4200)
```bash
cd frontend
npm install
npm start
```
Ouvrir http://localhost:4200. Le serveur Angular redirige `/api` et `/ws` vers le back (fichier `proxy.conf.json`).

Pour tester depuis un téléphone sur le même Wi-Fi, ouvrir `http://<IP-du-PC>:4200`.

### 3.3 Comptes de démonstration
| Rôle | E-mail | Mot de passe |
|---|---|---|
| Gérant (Salon Karim) | `karim@dourek.tn` | `dourek123` |
| Employé (Poste 2, Salon Karim) | `sami@dourek.tn` | `dourek123` |
| Gérant (Labo El Menzah) | `labo@dourek.tn` | `dourek123` |
| Client (Yassine Trabelsi) | `client@dourek.tn` | `dourek123` |

Ces comptes se saisissent à la main sur la page de connexion (client) ou sur « Espace pro » (gérant, employé) : il n'y a pas de bouton de remplissage automatique.

> Base PostgreSQL créée par une version précédente : la supprimer et la recréer (`DROP DATABASE dourek; CREATE DATABASE dourek;`), car un client n'a pas d'établissement (colonne désormais facultative).

Scénario conseillé :
1. Ouvrir l'écran employé avec `sami@dourek.tn`.
2. Dans un autre onglet ou sur un téléphone, se connecter avec `client@dourek.tn` puis prendre un ticket chez **Salon Karim**.
3. Cliquer sur **Appeler le suivant** : le ticket du client passe à « C'est votre tour ! » sans recharger la page.

### 3.4 Variables d'environnement (facultatives)
Aucune clé n'est écrite dans le code.

| Variable | Rôle | Défaut |
|---|---|---|
| `ANTHROPIC_API_KEY` | Clé de l'API Claude (assistant de recherche et formulaire par IA) | (vide) |
| `GEMINI_API_KEY` | Clé gratuite Google Gemini, utilisée s'il n'y a pas de clé Claude | (vide) |
| `DOUREK_GEMINI_MODELE` | Modèle Gemini essayé en premier. S'il est surchargé, retiré ou hors quota, le serveur essaie tout seul les autres modèles « flash » et garde celui qui répond. | `gemini-flash-lite-latest` |
| `DOUREK_ASSISTANT_MODELE` | Modèle Claude utilisé | `claude-opus-5-5` |
| `DOUREK_JWT_SECRET` | Secret de signature des jetons | valeur de développement |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Connexion PostgreSQL | `jdbc:postgresql://localhost:5432/dourek`, `dourek`, `dourek` |

**Activer l'IA (assistant de recherche et formulaire par IA).** L'IA est appelée par le serveur : il suffit de la configurer une fois sur la machine qui lance le back-end, et elle marche pour tous les visiteurs (ils ne voient jamais la clé). Ordre de choix :
1. `ANTHROPIC_API_KEY` définie → Claude (payant, https://console.anthropic.com) ;
2. sinon `GEMINI_API_KEY` définie → **Google Gemini, offre gratuite** ;
3. sinon → **mode local** (règles par mots-clés), indiqué à l'écran.

Gemini gratuit, pas à pas :
1. aller sur https://aistudio.google.com/apikey avec un compte Google et cliquer sur « Create API key » ;
2. dans le terminal du back-end (PowerShell) : `$env:GEMINI_API_KEY="votre-clé"` (la clé commence par `AIza` ou, pour les clés récentes, par `AQ.`) ;
3. lancer `mvn spring-boot:run` **dans ce même terminal**. Le journal affiche « IA de Dourek (assistant et formulaire) : API Gemini ».

**Sans terminal (pratique pour un correcteur)** : copier `backend/cle-ia.properties.exemple` en `backend/cle-ia.properties` et y coller la clé (`GEMINI_API_KEY=votre-clé`). Ce fichier est ignoré par Git : la clé n'est jamais publiée sur GitHub. On le transmet à part (par e-mail par exemple).

L'offre gratuite a une limite de requêtes par minute (une recherche avec l'IA fait deux appels : comprendre, puis expliquer) et Google peut utiliser les échanges pour améliorer ses modèles. Les modèles gratuits sont parfois surchargés (erreur 503) : le serveur passe alors au modèle suivant, et si aucun ne répond en 25 secondes il repasse en mode local.

**Adresse automatique** : à l'inscription pro, un clic sur la carte remplit l'adresse et le quartier grâce à Nominatim (OpenStreetMap), appelé par le back-end (`GET /api/geo/adresse`). Gratuit, sans clé, mais il faut Internet.

Exemple :
```bash
export ANTHROPIC_API_KEY=sk-ant-...      # Windows PowerShell : $env:ANTHROPIC_API_KEY="sk-ant-..."
mvn spring-boot:run
```

---

## 4. Organisation du code

### 4.1 Dossiers

```
backend/   API Spring Boot (package tn.supcom.dourek)
  model/        entités JPA : Etablissement, Prestation, Formulaire, Ticket, Utilisateur
  repository/   accès aux données (verrou pessimiste pour la file)
  service/      FileService, FormulaireService, RechercheService, AssistantService, FormulaireIaService, AuthService, TempsReel,
                ClaudeClient et GeminiClient (accès aux deux IA)
  web/          PublicController (/api), AuthController (/api/auth), ProController (/api/pro), GeoController (/api/geo),
                ErreursApi (erreurs au format { status, message })
  config/       sécurité JWT, WebSocket, données de démo
frontend/  application Angular
  src/app/core/     modèles, services API, authentification, temps réel
  src/app/shared/   en-tête, carte, champ dynamique, constructeur de formulaire
  src/app/pages/    accueil, recherche, établissement, ticket, compte client
  src/app/pro/      connexion, inscription, tableau de bord, formulaire, écran employé
```

### 4.2 Dépendances

| Côté | Dépendance | Rôle |
|---|---|---|
| Front | `@angular/core`, `common`, `router`, `forms`, `platform-browser` | Framework, routage, formulaires |
| Front | `@angular/cdk` | Glisser-déposer du constructeur de formulaire |
| Front | `@stomp/stompjs` | Temps réel (WebSocket STOMP) |
| Front | `leaflet` | Carte OpenStreetMap |
| Front | `rxjs` | Appels HTTP et flux temps réel |
| Front (dev) | `@angular/cli`, `@angular/build`, `typescript`, `vitest`, `prettier` | Compilation, serveur de développement, tests, mise en forme |
| Back | `spring-boot-starter-webmvc`, `-validation` | API REST et validation des données |
| Back | `spring-boot-starter-data-jpa`, `postgresql`, `h2` | Base de données (PostgreSQL, ou H2 en mémoire pour la démo) |
| Back | `spring-boot-starter-security`, `-oauth2-resource-server` | Rôles et jetons JWT |
| Back | `spring-boot-starter-websocket` | Diffusion des changements de file |
| Back | `anthropic-java` | SDK officiel de l'API Claude (facultatif) |
| Back | appel REST direct (aucune bibliothèque) | Google Gemini et Nominatim |
| Back (test) | `spring-boot-starter-test` (JUnit 5, AssertJ) | Tests automatiques |

### Principales routes de l'API
| Méthode | Route | Accès |
|---|---|---|
| GET | `/api/etablissements?q=&categorie=&lat=&lng=` | public |
| GET | `/api/etablissements/{slug}` | public |
| POST | `/api/etablissements/{slug}/tickets` | client connecté |
| GET / POST | `/api/tickets/{code}`, `/annuler`, `/laisser-passer` | client propriétaire du ticket |
| GET | `/api/client/tickets` | client connecté |
| GET | `/api/categories` | public |
| GET | `/api/geo/adresse?lat=&lng=` | public (adresse via OpenStreetMap) |
| POST | `/api/assistant` | public |
| POST | `/api/assistant/formulaire` | public (propose un formulaire, n'enregistre rien) |
| POST | `/api/auth/connexion`, `/api/auth/inscription`, `/api/auth/inscription-client` | public |
| GET / POST | `/api/pro/file`, `/api/pro/file/appeler-suivant`, `/servi`, `/absent`, `/sur-place` | gérant ou employé |
| GET / POST | `/api/pro/gerant/statistiques`, `/gerant/formulaire`, `/gerant/file/ouverture` | gérant |
| WS | `/ws` (STOMP), sujet `/topic/etablissements/{id}` | public |

---

## 5. Tests et qualité

### 5.1 Tests automatiques du back-end (JUnit)
```bash
cd backend
mvn test
```
37 tests, sans base de données ni clé d'IA :
- **recherche avec l'IA** : ce que notre code contrôle (identifiants inventés par le modèle ignorés, distance, attente et heure appliquées, files en pause jamais choisies, priorité du client), la liste complétée quand le modèle oublie un établissement d'un type, et le mode sans IA (« coiffure », « je veux boire quelque chose », « à moins de 3,5 km », « caffe ») ;
- **formulaire par IA** : contrôle de la proposition du modèle (types, options, doublons, longueurs) et règles du mode sans IA ;
- **recherche normale** : catégories devinées depuis le texte (« coiffure », « coupe femme », « prise de sang »…), mots utiles, fautes de frappe corrigées sans toucher aux vrais mots ;
- **mes tickets** : ordre d'affichage (ticket appelé, puis la plus courte attente, puis les anciens) ;
- **Gemini** : ordre des modèles de secours (flash-lite, puis flash, version stable la plus récente) et erreurs qui ne changent pas de modèle (clé refusée, schéma refusé).

### 5.2 Parcours testés (Playwright, Chrome)
| Scénario | Résultat |
|---|---|
| Un client prend un ticket chez Salon Karim, l'employé clique 6 fois sur « Appeler le suivant » | La page du client passe à « C'est votre tour ! » sans rechargement (WebSocket) |
| Le gérant glisse un champ « Date » dans le formulaire, le réordonne, publie la version 4 | Le formulaire du client affiche la v4 avec le nouveau champ |
| Champ obligatoire vide à la prise de ticket | Refus 400 avec un message clair |
| Employé qui appelle une route réservée au gérant | Refus 403 |
| Recherche normale « coiffure », puis « je veux boire quelque chose » | 5 salons listés (barbiers et coiffure femme) ; la phrase ne donne rien et la page propose de l'essayer avec l'IA |
| Recherche avec l'IA, vraie clé Gemini (13/11/2026) : « coiffure » | Les 5 salons sont listés, « Choix de l'assistant » sur le plus rapide, explication en 2 phrases, réponse en 3 secondes environ |
| Recherche avec l'IA, vraie clé : « je veux boire quelque chose » | Café des Jasmins (choix de l'assistant) et Le Patio, rien d'autre |
| Recherche avec l'IA, vraie clé : « une coiffure à moins de 1 km » | Étiquettes « Coiffure » et « à moins de 1 km », seuls les 2 salons à moins de 1 km restent |
| Recherche avec l'IA sans clé : « une barbe à Ennasr », « le barbier le mieux noté » | Même écran en mode local (mention affichée), classement selon la priorité |
| Le gérant écrit « Laboratoire : date de naissance, à jeun, ordonnance » et clique sur « Proposer un formulaire » | Les champs apparaissent dans le constructeur, « Annuler » rétablit l'ancien formulaire, la publication donne la v4 côté client |
| Même demande avec une vraie clé Gemini gratuite (13/11/2026) | Réponse en 1 à 2 secondes : « Date de naissance » (date), « Êtes-vous à jeun » (Oui/Non), « Type d'ordonnance » (liste) |
| Recherche avec l'IA, vraie clé : « je veux me raser la barbe vers Ennasr, pas trop cher » | Critères compris (se raser, Ennasr, petit prix), Barber Ennasr avec une explication |
| Modèle Gemini surchargé (503) ou retiré (404) | Le serveur passe au modèle suivant sans erreur pour le visiteur |
| IA Gemini (faux serveur de test) : modèle inconnu, schéma refusé, mauvaise clé | Autre modèle choisi, nouvel essai en JSON simple, puis repli en mode local |
| Ouverture du site sans compte (accueil, établissements, fiche, « Mes tickets ») | Page de connexion, sans onglet Accueil ni Établissements ; après connexion, retour à la page demandée |
| Connexion d'un client sans ticket, puis prise d'un ticket au labo et d'un autre au café | Accueil : « Vous n'avez pas de ticket en cours », puis le ticket du labo, puis celui du café en premier (il passe plus tôt) ; le clic ouvre Mes tickets dans le même ordre |
| Nouvel onglet, déconnexion, compte pro sur l'accueil | Nouvel onglet et déconnexion : page de connexion ; un pro voit une carte « Espace pro » à la place du ticket |
| Recherche « caffe » (faute de frappe) | Normale : les 2 cafés. Avec l'IA, vraie clé Gemini (14/11/2026, 13 essais dont « un caffe pas cher » et « kahwa ») : les 2 cafés et le salon de thé à chaque fois, y compris les 6 fois où le modèle n'en citait que 2 |
| Jeton expiré ou compte disparu (base de démo relancée) | Session fermée, renvoi à la page de connexion (client ou pro) |
| Carte agrandie sur la recherche, la fiche et l'inscription pro | Elle occupe toute la fenêtre, Échap la referme |
| Toutes les pages à 390 px (mobile) et 1440 px (bureau) | Aucun débordement horizontal |

### 5.3 Audit Lighthouse (build de production, profil mobile)
| Page | Performance | Accessibilité | Bonnes pratiques | SEO |
|---|---|---|---|---|
| Accueil | 94 | 100 | 100 | 100 |
| Recherche | 88 | 100 | 96\* | 100 |
| Établissement | 87 | 100 | 96\* | 100 |
| Connexion pro | 95 | 100 | 100 | 100 |
| Inscription pro | 91 | 100 | 100 | 100 |

\* Seul point restant : les tuiles de la carte OpenStreetMap n'étaient pas joignables depuis l'environnement de test.

Mesures appliquées :
- **Performance :** pages chargées à la demande (lazy loading), 93 Ko compressés au premier chargement, polices hébergées localement en woff2, aucune image lourde (icônes SVG).
- **Accessibilité :** balises sémantiques (`header`, `nav`, `main`, `footer`), contrastes vérifiés, libellés sur tous les champs, `aria-live` sur le ticket, navigation au clavier, `prefers-reduced-motion` respecté.

### 5.4 Responsive
- Trois tailles vérifiées : mobile (390 px), tablette (768 px), bureau (1440 px). Aucun défilement horizontal.
- Sous 900 px, le menu devient un bouton burger ; les grilles passent de 6 à 3 puis 2 colonnes ; la recherche et la carte s'empilent.
- Écran employé et ticket pensés d'abord pour le téléphone (gros boutons, grand numéro).

### 5.5 Navigateurs
| Navigateur | Moteur | Résultat |
|---|---|---|
| Chrome | Chromium (Blink) | Toutes les pages, prise de ticket, appel en direct, glisser-déposer : OK |
| Edge | Chromium (Blink), le même moteur que Chrome | OK par équivalence de moteur |
| Safari | WebKit (testé avec WebKitGTK 2.52 et WebDriver) | Toutes les pages, prise de ticket et appel en direct par WebSocket : OK |
| Firefox | Gecko | Testé à la main (14/11/2026) : connexion, recherche normale et avec l'IA, prise de ticket, suivi en direct : OK |

Le test WebKit a révélé un bug, corrigé depuis : si le visiteur ne répondait pas à la demande de localisation, la liste de la recherche restait vide. Elle s'affiche maintenant tout de suite avec la position par défaut, puis se met à jour si la position arrive.

### 5.6 Suivi des tâches
Les tâches ont été suivies dans les Issues GitHub du dépôt, une par fonctionnalité, avec son responsable et sa branche. Leur contenu est recopié en section 6.2.

---

## 6. Git et travail en équipe

### 6.1 Méthode
- Un dépôt GitHub partagé : `elyesmsolly/ProjetWeb_Elyes-Aziz`.
- **Une branche par fonctionnalité** (`feature/...`, `feat/...`), par correction (`fix/...`) ou pour la documentation (`docs/...`), créée depuis `main`.
- Commits courts et explicites, en français, qui disent ce qui change (« Suivi du ticket en temps réel », « Clé d'IA dans un fichier local ignoré par Git »).
- Chaque branche est relue, testée (JUnit et parcours dans le navigateur), puis **fusionnée dans `main` avec un commit de fusion** (`git merge --no-ff`) : l'historique garde la trace de chaque fonctionnalité.
- `main` reste toujours lançable. Les secrets (clé d'IA) ne sont jamais commités : `cle-ia.properties` est dans `.gitignore`.
- Les commits ont été envoyés depuis le compte GitHub d'Elyes ; la répartition du travail est celle des tâches ci-dessous.

### 6.2 Répartition des tâches (Issues GitHub)

| Tâche | Responsable | Branche(s) |
|---|---|---|
| #1 API Spring Boot : file d'attente, tickets, JWT, WebSocket | Elyes (back-end) | `feature/backend-api` |
| #2 Thème « Ton tour », en-tête et services Angular | Elyes et Aziz (intégration du design) | `feature/frontend-base` |
| #3 Accueil, recherche avec filtres et carte, page établissement | Aziz (côté client) | `feature/recherche-etablissement` |
| #4 Suivi du ticket en temps réel | Aziz (côté client) | `feature/ticket-temps-reel` |
| #5 Espace pro : connexion, tableau de bord, écran employé | Elyes (côté gérant) | `feature/espace-pro-employe` |
| #6 Constructeur de formulaire en glisser-déposer et inscription pro | Elyes (côté gérant) | `feature/constructeur-formulaire` |
| #7 Assistant de recherche IA | Aziz (assistant IA) | `feature/backend-api`, `feature/recherche-etablissement` |
| #8 README : framework, fonctionnalités, lancement | Elyes et Aziz | `docs/readme` |
| #9 Tests : performance, accessibilité et multi-navigateurs | Elyes et Aziz | `feature/tests-qualite`, `fix/recherche-localisation` |
| Améliorations après démonstration (compte client, IA Gemini, formulaire par IA, connexion d'abord) | Elyes et Aziz | branches 10 à 18 ci-dessous |

### 6.3 Branches (dans l'ordre de fusion)

| # | Branche | Contenu |
|---|---|---|
| 1 | `feature/backend-api` | API Spring Boot, base de données, sécurité, temps réel, assistant IA (back) |
| 2 | `feature/frontend-base` | Thème (charte « Ton tour »), en-tête, services, routage |
| 3 | `feature/recherche-etablissement` | Accueil, recherche, carte, assistant (front), page établissement |
| 4 | `feature/ticket-temps-reel` | Prise de ticket et suivi en direct |
| 5 | `feature/espace-pro-employe` | Connexion, tableau de bord, écran employé |
| 6 | `feature/constructeur-formulaire` | Formulaire en glisser-déposer, inscription du gérant en 5 étapes |
| 7 | `docs/readme` | Premier README |
| 8 | `feature/tests-qualite` | Corrections d'accessibilité et section Tests |
| 9 | `fix/recherche-localisation` | Recherche affichée même sans réponse à la demande de localisation |
| 10 | `feature/compte-client` | Compte client, ticket réservé au client connecté |
| 11 | `feature/assistant-gemini` | IA Google Gemini gratuite quand il n'y a pas de clé Claude |
| 12 | `feature/cle-ia-fichier` | Clé d'IA lue dans un fichier local ignoré par Git |
| 13 | `feature/formulaire-ia` | Formulaire client proposé par l'IA à partir d'une phrase du gérant |
| 14 | `fix/verification-generale` | Corrections après une revue complète |
| 15 | `fix/gemini-modele-disponible` | Modèle Gemini disponible choisi automatiquement |
| 16 | `feat/recherche-ia-intelligente` | Deux boutons de recherche (normale, avec l'IA), choix de l'assistant |
| 17 | `feat/connexion-d-abord` | Connexion obligatoire avant le site, vrai ticket sur l'accueil, fautes de frappe corrigées |
| 18 | `docs/readme-complet` | Ce README complet (Git, pages, lancement Windows, Firefox) |

### 6.4 Historique des commits (`git log`, hors commits de fusion)

| Date | Branche | Commit |
|---|---|---|
| 2026-11-13 | `main` | Initialisation du projet : front Angular et README |
| 2026-11-13 | `feature/backend-api` | API Spring Boot : file d'attente, tickets, formulaires versionnés, JWT, WebSocket, assistant |
| 2026-11-13 | `feature/backend-api` | API : identifiant d'établissement dans la vue ticket, messages d'erreur et données de démo |
| 2026-11-13 | `feature/frontend-base` | Front : thème « Ton tour », mise en page publique et services de base |
| 2026-11-13 | `feature/recherche-etablissement` | Recherche et page établissement |
| 2026-11-13 | `feature/ticket-temps-reel` | Suivi du ticket en temps réel |
| 2026-11-13 | `feature/espace-pro-employe` | Espace pro : connexion, tableau de bord et écran employé |
| 2026-11-13 | `feature/constructeur-formulaire` | Constructeur de formulaire en glisser-déposer et inscription pro |
| 2026-11-13 | `docs/readme` | README : choix du framework, fonctionnalités, lancement et comptes de démo |
| 2026-11-13 | `feature/tests-qualite` | Tests et qualité : corrections d'accessibilité et rapport Lighthouse |
| 2026-11-13 | `fix/recherche-localisation` | Recherche : ne plus attendre la réponse à la demande de localisation |
| 2026-11-13 | `feature/compte-client` | Compte client, ticket réservé au client connecté, recherche corrigée |
| 2026-11-13 | `feature/compte-client` | README : compte client, adresse automatique, activation de l'IA |
| 2026-11-13 | `feature/assistant-gemini` | Assistant : Google Gemini (offre gratuite) si pas de clé Claude |
| 2026-11-13 | `feature/cle-ia-fichier` | Clé d'IA dans un fichier local ignoré par Git (cle-ia.properties) |
| 2026-11-13 | `feature/formulaire-ia` | Formulaire client proposé par l'IA à partir d'une phrase du gérant |
| 2026-11-13 | `fix/verification-generale` | Corrections après revue complète des derniers changements |
| 2026-11-13 | `fix/verification-generale` | README : formulaire par IA, tests JUnit et scénarios vérifiés |
| 2026-11-13 | `fix/gemini-modele-disponible` | Gemini : modèle flash-lite par défaut et bascule automatique vers un autre modèle |
| 2026-11-13 | `feat/recherche-ia-intelligente` | Recherche avec l'IA : elle choisit parmi les établissements inscrits |
| 2026-11-13 | `feat/recherche-ia-intelligente` | Deux boutons de recherche : normale ou avec l'IA |
| 2026-11-13 | `feat/recherche-ia-intelligente` | README : recherche normale et recherche avec l'IA, 28 tests, essais avec le vrai Gemini |
| 2026-11-14 | `feat/connexion-d-abord` | Connexion d'abord : le site s'ouvre sur la page de connexion |
| 2026-11-14 | `feat/connexion-d-abord` | Accueil : le vrai ticket du compte connecté, page Mes tickets |
| 2026-11-14 | `feat/connexion-d-abord` | Recherche : fautes de frappe corrigées, liste d'un type toujours complète avec l'IA |
| 2026-11-14 | `feat/connexion-d-abord` | README : connexion d'abord, vrai ticket sur l'accueil, fautes de frappe, 37 tests |
| 2026-11-14 | `docs/readme-complet` | README : pages, lancement sous Windows, dépendances, Firefox, Git et répartition des tâches |

Chaque branche est ensuite fusionnée dans `main` par un commit « Fusion de *branche* dans main ».
