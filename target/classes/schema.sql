CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(40) PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  email VARCHAR(180) UNIQUE NOT NULL,
  password VARCHAR(120) NOT NULL,
  role ENUM('client','developer','admin') NOT NULL,
  company VARCHAR(150),
  title VARCHAR(150),
  domain VARCHAR(100),
  skills TEXT,
  level VARCHAR(50),
  rate DECIMAL(10,2) DEFAULT 0,
  availability VARCHAR(50) DEFAULT 'available',
  image TEXT,
  bio TEXT,
  projects TEXT,
  rating DECIMAL(4,2) DEFAULT 0,
  rating_count INT DEFAULT 0,
  reputation INT DEFAULT 50,
  missions_completed INT DEFAULT 0,
  refusals INT DEFAULT 0,
  delays INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS missions (
  id VARCHAR(40) PRIMARY KEY,
  title VARCHAR(220) NOT NULL,
  description TEXT,
  cleaned_description TEXT,
  category VARCHAR(80),
  extracted_skills TEXT,
  complexity_score DECIMAL(5,2) DEFAULT 0,
  complexity_label VARCHAR(50),
  risk_score DECIMAL(5,2) DEFAULT 0,
  estimated_budget_min DECIMAL(10,2) DEFAULT 0,
  estimated_budget_max DECIMAL(10,2) DEFAULT 0,
  estimated_days INT DEFAULT 0,
  budget DECIMAL(10,2) DEFAULT 0,
  skills TEXT,
  status VARCHAR(80) DEFAULT 'open',
  owner_email VARCHAR(180),
  client_id VARCHAR(40),
  assigned_developer_id VARCHAR(40),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE SET NULL,
  FOREIGN KEY (assigned_developer_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS ratings (
  id VARCHAR(40) PRIMARY KEY,
  developer_id VARCHAR(40),
  client_email VARCHAR(180),
  rating INT,
  text TEXT,
  sentiment VARCHAR(30),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (developer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS deliveries (
  id VARCHAR(40) PRIMARY KEY,
  mission_id VARCHAR(40),
  developer_id VARCHAR(40),
  description TEXT,
  link TEXT,
  quality_score DECIMAL(5,2) DEFAULT 0,
  status VARCHAR(60) DEFAULT 'delivered',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE,
  FOREIGN KEY (developer_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS notifications (
  id VARCHAR(40) PRIMARY KEY,
  recipient_email VARCHAR(180),
  message TEXT,
  type VARCHAR(80),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_logs (
  id VARCHAR(40) PRIMARY KEY,
  recipient_email VARCHAR(180),
  subject VARCHAR(250),
  body TEXT,
  status VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workflow_events (
  id VARCHAR(40) PRIMARY KEY,
  message TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO users(id,name,email,password,role,company,title,domain,skills,level,rate,availability,image,bio,projects,rating,rating_count,reputation,missions_completed) VALUES
('admin1','Administrateur','admin@system.local','1234','admin',NULL,'Admin plateforme','Management','Admin,Dashboard','Senior',0,'available','https://images.unsplash.com/photo-1556157382-97eda2d62296?w=400','Compte administrateur','[]',5,1,90,0),
('client1','Client Demo','client@demo.com','1234','client','Demo Company',NULL,NULL,NULL,NULL,0,'available','https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400','Client de démonstration','[]',0,0,50,0),
('d1','Ahmed Trabelsi','ahmed@dev.com','1234','developer',NULL,'Développeur Full Stack','Web','Angular,Spring Boot,MySQL,API,Dashboard','Senior',80,'available','https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400','Expert Angular/Spring avec expérience dashboards.','["Dashboard BI","Application missions Angular","API REST Spring"]',4.8,12,92,7),
('d2','Sarra Mansouri','sarra@dev.com','1234','developer',NULL,'Data Analyst / BI','Data','Python,SQL,Machine Learning,Dashboard,PowerBI','Confirmé',65,'available','https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400','Spécialiste data, nettoyage, KPIs et prédiction.','["Dashboard KPI","Nettoyage données clients","Modèle prédiction"]',4.6,8,86,5),
('d3','Youssef Ben Ali','youssef@dev.com','1234','developer',NULL,'Développeur Mobile','Mobile','Flutter,Firebase,API,UI UX','Junior',45,'busy','https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400','Développeur mobile et intégration API.','["App livraison","App mobile freelance"]',4.1,5,72,2);

INSERT IGNORE INTO missions(id,title,description,cleaned_description,category,extracted_skills,complexity_score,complexity_label,risk_score,estimated_budget_min,estimated_budget_max,estimated_days,budget,skills,status,owner_email,client_id) VALUES
('m1','Dashboard Angular pour suivi missions','Créer un dashboard Angular avec statistiques, API Spring Boot et base MySQL.','creer dashboard angular statistiques api spring boot base mysql','Frontend','Angular,Dashboard,Spring Boot,MySQL,API',78,'Complexe',32,900,1400,12,1000,'Angular,Dashboard,Spring Boot,MySQL','open','client@demo.com','client1'),
('m2','Module Data Cleaning et prédiction','Ajouter nettoyage de données, score de risque et prédiction réussite mission.','ajouter nettoyage donnees score risque prediction reussite mission','Data','Python,SQL,Machine Learning,Prediction,Data Cleaning',85,'Complexe',28,1000,1600,14,1200,'Python,SQL,Machine Learning,Data Cleaning','open','client@demo.com','client1');
