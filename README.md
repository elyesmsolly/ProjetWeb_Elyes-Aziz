# Dourek : faites la queue depuis chez vous

Plateforme de files d'attente virtuelles pour les petits commerces (barbiers, salons, laboratoires, agences, administrations).
Le client trouve l'établissement qui a la plus courte attente, prend son ticket à distance et suit son tour en direct.
Le gérant crée sa file, compose le formulaire que remplit le client, et l'employé appelle le suivant depuis son écran.

Projet Web, Objectif 3, SUP'COM 2026/2027 (encadrante : Dr. Maroua Nouioua).
Réalisé par **Med Elyes Msolly** et **Med Aziz Ghazouani**.

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
| Assistant | **API Claude** (SDK officiel `anthropic-java`) | Comprend une demande en langage naturel. Sans clé, une analyse locale prend le relais. |

Tout tourne en local, sans déploiement.

---

## 2. Fonctionnalités réalisées

### Côté client
- **Compte client** (`/connexion`, `/inscription`) : nom, e-mail, téléphone et mot de passe.
  - Prendre un ticket et voir « Mon ticket » demande d'être connecté en tant que client.
  - `/ticket` ouvre le ticket en cours, sinon la liste des derniers tickets du client.
  - Un client ne peut voir que ses propres tickets (403 sinon).
- **Accueil** : recherche par service et lieu, catégories, établissements proches avec leur attente en direct.
- **Recherche** (`/etablissements`) :
  - filtres par catégorie, « ouvert maintenant », attente < 20 min, < 1 km et note 4+ ;
  - tri, vue liste ou carte ;
  - temps d'attente mis à jour en direct ;
  - la recherche libre devine la catégorie (« coiffeur » → barbiers) puis garde les établissements qui correspondent le mieux aux autres mots ;
  - les catégories « Autres » créées par les gérants (ex. Pressing) apparaissent comme filtres.
- **Carte** (OpenStreetMap + Leaflet, gratuit, sans clé) : bouton plein écran, touche Échap pour fermer.
- **Assistant de recherche** : « une coupe avant 18 h près de l'Ariana, pas trop cher ».
  - IA au choix : Claude, ou Google Gemini (gratuit) ; sans clé, analyse locale.
  - L'IA extrait les critères (sortie structurée).
  - **Notre code classe** les établissements selon la distance, l'attente et l'avis.
  - L'IA explique ensuite le classement : elle ne décide pas.
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
Les tables sont créées automatiquement. Au premier lancement, des données de démonstration sont insérées : 8 établissements à l'Ariana, des tickets et un historique.

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
| `DOUREK_GEMINI_MODELE` | Modèle Gemini. S'il n'existe plus, le serveur choisit tout seul le modèle « flash » disponible. | `gemini-flash-latest` |
| `DOUREK_ASSISTANT_MODELE` | Modèle Claude utilisé | `claude-opus-5-5` |
| `DOUREK_JWT_SECRET` | Secret de signature des jetons | valeur de développement |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Connexion PostgreSQL | `jdbc:postgresql://localhost:5432/dourek`, `dourek`, `dourek` |

**Activer l'IA (assistant de recherche et formulaire par IA).** L'IA est appelée par le serveur : il suffit de la configurer une fois sur la machine qui lance le back-end, et elle marche pour tous les visiteurs (ils ne voient jamais la clé). Ordre de choix :
1. `ANTHROPIC_API_KEY` définie → Claude (payant, https://console.anthropic.com) ;
2. sinon `GEMINI_API_KEY` définie → **Google Gemini, offre gratuite** ;
3. sinon → **mode local** (règles par mots-clés), indiqué à l'écran.

Gemini gratuit, pas à pas :
1. aller sur https://aistudio.google.com/apikey avec un compte Google et cliquer sur « Create API key » ;
2. dans le terminal du back-end (PowerShell) : `$env:GEMINI_API_KEY="AIza..."` ;
3. lancer `mvn spring-boot:run` **dans ce même terminal**. Le journal affiche « IA de Dourek (assistant et formulaire) : API Gemini ».

**Sans terminal (pratique pour un correcteur)** : copier `backend/cle-ia.properties.exemple` en `backend/cle-ia.properties` et y coller la clé (`GEMINI_API_KEY=AIza...`). Ce fichier est ignoré par Git : la clé n'est jamais publiée sur GitHub. On le transmet à part (par e-mail par exemple).

L'offre gratuite a une limite de requêtes par minute et Google peut utiliser les échanges pour améliorer ses modèles. Si Gemini ne répond pas, le serveur repasse en mode local.

**Adresse automatique** : à l'inscription pro, un clic sur la carte remplit l'adresse et le quartier grâce à Nominatim (OpenStreetMap), appelé par le back-end (`GET /api/geo/adresse`). Gratuit, sans clé, mais il faut Internet.

Exemple :
```bash
export ANTHROPIC_API_KEY=sk-ant-...      # Windows PowerShell : $env:ANTHROPIC_API_KEY="sk-ant-..."
mvn spring-boot:run
```

---

## 4. Organisation du code

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

### Tests automatiques du back-end (JUnit)
```bash
cd backend
mvn test
```
15 tests, sans base de données ni clé d'IA :
- **formulaire par IA** : contrôle de la proposition du modèle (types, options, doublons, longueurs) et règles du mode sans IA ;
- **recherche** : catégorie devinée depuis le texte (« coiffeur », « prise de sang »…), mots utiles, critères de l'assistant sans IA ;
- **Gemini** : choix du modèle quand celui de la configuration n'existe plus.

### Parcours testés (Playwright, Chrome)
| Scénario | Résultat |
|---|---|
| Un client prend un ticket chez Salon Karim, l'employé clique 6 fois sur « Appeler le suivant » | La page du client passe à « C'est votre tour ! » sans rechargement (WebSocket) |
| Le gérant glisse un champ « Date » dans le formulaire, le réordonne, publie la version 4 | Le formulaire du client affiche la v4 avec le nouveau champ |
| Champ obligatoire vide à la prise de ticket | Refus 400 avec un message clair |
| Employé qui appelle une route réservée au gérant | Refus 403 |
| Assistant : « une barbe pas loin, le plus vite possible » | Classement et explication (mode local sans clé) |
| Le gérant écrit « Laboratoire : date de naissance, à jeun, ordonnance » et clique sur « Proposer un formulaire » | Les champs apparaissent dans le constructeur, « Annuler » rétablit l'ancien formulaire, la publication donne la v4 côté client |
| IA Gemini (faux serveur de test) : modèle inconnu, schéma refusé, mauvaise clé | Autre modèle choisi, nouvel essai en JSON simple, puis repli en mode local |
| Client non connecté qui ouvre « Mon ticket » ou une fiche | Renvoi à la connexion, puis retour à la page demandée |
| Jeton expiré ou compte disparu (base de démo relancée) | Session fermée, renvoi à la connexion, pages publiques toujours lisibles |
| Carte agrandie sur la recherche, la fiche et l'inscription pro | Elle occupe toute la fenêtre, Échap la referme |
| Toutes les pages à 390 px (mobile) et 1440 px (bureau) | Aucun débordement horizontal |

### Audit Lighthouse (build de production, profil mobile)
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

### Navigateurs
| Navigateur | Moteur | Résultat |
|---|---|---|
| Chrome | Chromium (Blink) | Toutes les pages, prise de ticket, appel en direct, glisser-déposer : OK |
| Edge | Chromium (Blink), le même moteur que Chrome | OK par équivalence de moteur |
| Safari | WebKit (testé avec WebKitGTK 2.52 et WebDriver) | Toutes les pages, prise de ticket et appel en direct par WebSocket : OK |
| Firefox | Gecko | À vérifier à la main avant le rendu |

Le test WebKit a révélé un bug, corrigé depuis : si le visiteur ne répondait pas à la demande de localisation, la liste de la recherche restait vide. Elle s'affiche maintenant tout de suite avec la position par défaut, puis se met à jour si la position arrive.

### Suivi des tâches
Les tâches sont suivies dans les [Issues GitHub](https://github.com/elyesmsolly/ProjetWeb_Elyes-Aziz/issues?q=is%3Aissue), une par fonctionnalité, avec le responsable et la branche.

---

## 6. Branches

Chaque fonctionnalité a été développée sur sa propre branche, puis fusionnée dans `main` :

| Branche | Contenu |
|---|---|
| `feature/backend-api` | API Spring Boot, base de données, sécurité, temps réel, assistant IA (back) |
| `feature/frontend-base` | Thème (charte « Ton tour »), en-tête, services, routage |
| `feature/recherche-etablissement` | Accueil, recherche, carte, assistant (front), page établissement |
| `feature/ticket-temps-reel` | Prise de ticket et suivi en direct |
| `feature/espace-pro-employe` | Connexion, tableau de bord, écran employé |
| `feature/constructeur-formulaire` | Formulaire en glisser-déposer, inscription du gérant en 5 étapes |
| `docs/readme` | Ce README |
| `feature/tests-qualite` | Corrections d'accessibilité et section Tests |
| `fix/recherche-localisation` | Recherche affichée même sans réponse à la demande de localisation |
