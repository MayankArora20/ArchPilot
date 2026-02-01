# pgAdmin Setup Guide for ArchPilot

Since you have pgAdmin installed, here's how to set up the database manually:

## Step 1: Create Database

1. Open **pgAdmin**
2. Connect to your PostgreSQL server (usually localhost)
3. Right-click on **"Databases"** in the left panel
4. Select **"Create" > "Database..."**
5. Enter database name: `archpilot`
6. Click **"Save"**

## Step 2: Create User

1. Right-click on **"Login/Group Roles"** in the left panel
2. Select **"Create" > "Login/Group Role..."**
3. In the **"General"** tab:
   - Name: `archpilot_user`
4. In the **"Definition"** tab:
   - Password: `archpilot_password`
5. In the **"Privileges"** tab:
   - Check: **"Can login?"**
   - Check: **"Create databases?"** (optional)
6. Click **"Save"**

## Step 3: Grant Database Privileges

1. Right-click on the **"archpilot"** database
2. Select **"Properties"**
3. Go to the **"Security"** tab
4. Click the **"+"** button to add a new privilege
5. Select **"archpilot_user"** from the Grantee dropdown
6. Set Privileges to: **"ALL"**
7. Click **"Save"**

## Step 4: Grant Schema Privileges

1. Expand the **"archpilot"** database
2. Expand **"Schemas"**
3. Right-click on **"public"** schema
4. Select **"Properties"**
5. Go to the **"Security"** tab
6. Click **"+"** to add privileges for **"archpilot_user"**
7. Set all privileges (Usage, Create, etc.)
8. Click **"Save"**

## Step 5: Test Connection

1. Right-click on **"Servers"** in the left panel
2. Select **"Create" > "Server..."**
3. In **"General"** tab:
   - Name: `ArchPilot Test`
4. In **"Connection"** tab:
   - Host: `localhost`
   - Port: `5432`
   - Database: `archpilot`
   - Username: `archpilot_user`
   - Password: `archpilot_password`
5. Click **"Save"**

If the connection succeeds, you're ready to run the application!

## Quick SQL Alternative

If you prefer SQL, you can also run this in pgAdmin's Query Tool:

```sql
-- Connect to postgres database first, then run:
CREATE DATABASE archpilot;
CREATE USER archpilot_user WITH PASSWORD 'archpilot_password';
GRANT ALL PRIVILEGES ON DATABASE archpilot TO archpilot_user;

-- Then connect to archpilot database and run:
GRANT ALL ON SCHEMA public TO archpilot_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO archpilot_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO archpilot_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO archpilot_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO archpilot_user;
```

After setup, run your application with: `run-app.bat`