USE [master]
GO

CREATE DATABASE [tigasedb];
GO

CREATE LOGIN [tigase] WITH PASSWORD=N'tigase12', DEFAULT_DATABASE=[tigasedb]
GO

USE [tigasedb]
GO

ALTER AUTHORIZATION ON DATABASE::tigasedb TO tigase;
GO