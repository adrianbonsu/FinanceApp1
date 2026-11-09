-- ========================================================
-- Database Migration & Seed Script for Money Mentor App
-- Generated and Normalized from 'Book 3.xlsx'
-- ========================================================

PRAGMA foreign_keys = ON;

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. CATEGORIES TABLE
CREATE TABLE IF NOT EXISTS categories (
    category_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES users(user_id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    is_default INTEGER DEFAULT 1
);

-- 3. VENDORS TABLE
CREATE TABLE IF NOT EXISTS vendors (
    vendor_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    default_category_id INTEGER REFERENCES categories(category_id) ON DELETE SET NULL
);

-- 4. CATEGORY BUDGETS TABLE
CREATE TABLE IF NOT EXISTS category_budgets (
    budget_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories(category_id) ON DELETE CASCADE,
    monthly_limit NUMERIC(10, 2) NOT NULL CHECK (monthly_limit > 0),
    month_year DATE NOT NULL,
    UNIQUE (user_id, category_id, month_year)
);

-- 5. EXPENSES TABLE
CREATE TABLE IF NOT EXISTS expenses (
    expense_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    category_id INTEGER REFERENCES categories(category_id) ON DELETE SET NULL,
    vendor_id TEXT REFERENCES vendors(vendor_id) ON DELETE SET NULL,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    expense_date DATE NOT NULL,
    payment_method TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. SAVINGS GOALS TABLE
CREATE TABLE IF NOT EXISTS goals (
    goal_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    target_amount NUMERIC(10, 2) NOT NULL CHECK (target_amount > 0),
    current_amount NUMERIC(10, 2) DEFAULT 0.00 CHECK (current_amount >= 0),
    target_date DATE,
    status TEXT DEFAULT 'in_progress',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- INDEXES FOR PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_expenses_user_date ON expenses(user_id, expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(category_id);
CREATE INDEX IF NOT EXISTS idx_goals_user ON goals(user_id);

-- SEED DATA: USERS (Cleaned & Unique IDs)
INSERT INTO users (user_id, first_name, last_name, email, password_hash) VALUES (2026781, 'Clinton', 'Davis', 'clinton@abc.edu', '316fbdf878af0cd6d5383836469ca8913cb3af7987812543849430fd95c04c63');
INSERT INTO users (user_id, first_name, last_name, email, password_hash) VALUES (2026782, 'Alex', 'Williams', 'alex@abc.edu', '2aba4e9fba8592dbe8a6295a82c32fe913f7504c5a970f0441c43dd30995713e');
INSERT INTO users (user_id, first_name, last_name, email, password_hash) VALUES (2026783, 'Adriana', 'Rodriguez', 'adriana@abc.edu', 'cf55f48a0a9166e739fc43e0376e251b1107daa32353ef3a299c747a0cd30297');

-- SEED DATA: CATEGORIES
INSERT INTO categories (category_id, user_id, name, is_default) VALUES (1, NULL, 'Groceries & Food', 1);
INSERT INTO categories (category_id, user_id, name, is_default) VALUES (2, NULL, 'Textbooks & Supplies', 1);
INSERT INTO categories (category_id, user_id, name, is_default) VALUES (3, NULL, 'Housing & Utilities', 1);
INSERT INTO categories (category_id, user_id, name, is_default) VALUES (4, NULL, 'Entertainment & Subscriptions', 1);
INSERT INTO categories (category_id, user_id, name, is_default) VALUES (5, NULL, 'Transport & Commute', 1);
INSERT INTO categories (category_id, user_id, name, is_default) VALUES (6, NULL, 'Coffee & Snacks', 1);
INSERT INTO categories (category_id, user_id, name, is_default) VALUES (7, NULL, 'Fashion and clothing', 1);
INSERT INTO categories (category_id, user_id, name, is_default) VALUES (8, NULL, 'Other miscellaneous expenses', 1);

-- SEED DATA: VENDORS (Original + Missing ones from expenses)
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v1', 'Campus Bookstore', 2);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v2', 'Trader Joe''s', 1);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v3', 'Campus Dining Hall', 1);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v4', 'Spotify', 4);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v5', 'Local Transit Pass', 5);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v6', 'Starbucks', 6);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v7', 'Staples', 2);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v8', 'Uniqlo', 7);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v9', 'Nordstrom', 7);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v10', 'Chipotle', 1);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v11', 'Uber', 5);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v12', 'Steam', 4);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v13', 'Campus Laundry', 3);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v14', 'Dunkin''', 6);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v15', 'CVS Pharmacy', 8);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v16', 'University Printing', 2);
INSERT INTO vendors (vendor_id, name, default_category_id) VALUES ('v17', 'Target', 1);

-- SEED DATA: MONTHLY BUDGETS (November 2026)
INSERT INTO category_budgets (budget_id, user_id, category_id, monthly_limit, month_year) VALUES (1, 2026781, 1, 300.00, '2026-11-01');
INSERT INTO category_budgets (budget_id, user_id, category_id, monthly_limit, month_year) VALUES (2, 2026781, 2, 150.00, '2026-11-01');
INSERT INTO category_budgets (budget_id, user_id, category_id, monthly_limit, month_year) VALUES (3, 2026781, 4, 50.00, '2026-11-01');
INSERT INTO category_budgets (budget_id, user_id, category_id, monthly_limit, month_year) VALUES (4, 2026781, 6, 40.00, '2026-11-01');
INSERT INTO category_budgets (budget_id, user_id, category_id, monthly_limit, month_year) VALUES (5, 2026781, 7, 200.00, '2026-11-01');

-- SEED DATA: EXPENSES (Normalized with vendor_id foreign keys)
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (1, 2026781, 2, 'v7', 84.50, '2026-11-01', 'Debit Card', 'Stationary supplies');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (2, 2026781, 1, 'v2', 42.15, '2026-11-02', 'Debit Card', 'Weekly grocery run');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (3, 2026781, 6, 'v6', 5.75, '2026-11-03', 'Student ID Card', 'Iced latte study session');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (4, 2026781, 4, 'v4', 5.99, '2026-11-04', 'Debit Card', 'Student discounted subscription');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (5, 2026781, 1, 'v3', 11.20, '2026-11-05', 'Student ID Card', 'Late lunch on campus');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (6, 2026781, 1, 'v10', 14.85, '2026-11-06', 'Debit Card', 'Burrito bowl after lab session');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (7, 2026781, 5, 'v11', 18.50, '2026-11-07', 'Credit Card', 'Rideshare home from late group project');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (8, 2026781, 4, 'v12', 19.99, '2026-11-08', 'Debit Card', 'Midterm stress-relief indie game');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (9, 2026781, 3, 'v13', 7.50, '2026-11-09', 'Student ID Card', 'Dorm laundry wash and dry tokens');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (10, 2026781, 6, 'v14', 4.25, '2026-11-10', 'Student ID Card', 'Medium iced coffee and donut');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (11, 2026781, 8, 'v15', 12.30, '2026-11-11', 'Debit Card', 'Flu medicine, throat lozenges, tissues');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (12, 2026781, 2, 'v16', 6.40, '2026-11-12', 'Student ID Card', 'Printing poster presentation and report');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (13, 2026781, 1, 'v17', 38.70, '2026-11-14', 'Debit Card', 'Dorm cleaning supplies and bulk snacks');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (14, 2026781, 5, 'v5', 25.00, '2026-11-15', 'Debit Card', 'Bi-weekly campus bus pass reload');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (15, 2026781, 6, 'v6', 6.10, '2026-11-16', 'Debit Card', 'Nitro cold brew before morning exam');
INSERT INTO expenses (expense_id, user_id, category_id, vendor_id, amount, expense_date, payment_method, notes) VALUES (16, 2026781, 7, 'v8', 178.35, '2026-11-17', 'Debit Card', 'Winter Shopping');

-- SEED DATA: FINANCIAL GOALS
INSERT INTO goals (user_id, title, target_amount, current_amount, target_date, status) VALUES (2026781, 'Spring Break Trip', 450.00, 150.00, '2027-03-15', 'in_progress');
INSERT INTO goals (user_id, title, target_amount, current_amount, target_date, status) VALUES (2026781, 'Emergency Buffer', 300.00, 300.00, '2026-10-15', 'completed');