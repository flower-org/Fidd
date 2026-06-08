-- Adds a full-text index on the fidd description column after Hibernate creates the table
CREATE FULLTEXT INDEX idx_fidd_desc ON fidd(description);
