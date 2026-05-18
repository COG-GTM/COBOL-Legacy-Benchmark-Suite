//! OpenAPI specification (manually assembled).
//!
//! Provides a `/api/openapi.json` endpoint that returns the OpenAPI 3.0
//! specification for the portfolio management API.

use serde_json::{json, Value};

/// Build the OpenAPI 3.0 specification as a JSON value.
pub fn spec() -> Value {
    json!({
        "openapi": "3.0.3",
        "info": {
            "title": "Portfolio Management API",
            "description": "REST API for the Investment Portfolio Management System — translated from COBOL CICS/DB2 online programs (INQONLN, SECMGR).",
            "version": "0.1.0",
            "contact": { "name": "COG-GTM" }
        },
        "servers": [
            { "url": "http://localhost:3000", "description": "Local development" }
        ],
        "components": {
            "securitySchemes": {
                "bearerAuth": {
                    "type": "http",
                    "scheme": "bearer",
                    "bearerFormat": "JWT"
                }
            },
            "schemas": {
                "Portfolio": {
                    "type": "object",
                    "properties": {
                        "id":           { "type": "string" },
                        "accountNo":    { "type": "string" },
                        "clientName":   { "type": "string" },
                        "clientType":   { "type": "string", "enum": ["I", "C", "T"] },
                        "status":       { "type": "string", "enum": ["A", "C", "S"] },
                        "totalValue":   { "type": "number", "description": "Decimal value" },
                        "cashBalance":  { "type": "number", "description": "Decimal value" },
                        "createDate":   { "type": "string", "format": "date", "nullable": true },
                        "lastMaint":    { "type": "string", "format": "date", "nullable": true },
                        "lastUser":     { "type": "string" },
                        "lastTrans":    { "type": "string", "format": "date", "nullable": true }
                    }
                },
                "CreatePortfolio": {
                    "type": "object",
                    "required": ["accountNo", "clientName", "clientType"],
                    "properties": {
                        "accountNo":   { "type": "string", "maxLength": 10 },
                        "clientName":  { "type": "string", "maxLength": 30 },
                        "clientType":  { "type": "string", "enum": ["I", "C", "T"] },
                        "cashBalance": { "type": "number", "default": 0 }
                    }
                },
                "UpdatePortfolio": {
                    "type": "object",
                    "properties": {
                        "clientName":  { "type": "string" },
                        "clientType":  { "type": "string", "enum": ["I", "C", "T"] },
                        "status":      { "type": "string", "enum": ["A", "C", "S"] },
                        "cashBalance": { "type": "number" }
                    }
                },
                "Position": {
                    "type": "object",
                    "properties": {
                        "portfolioId":        { "type": "string" },
                        "investmentId":       { "type": "string" },
                        "quantity":           { "type": "number" },
                        "costBasis":          { "type": "number" },
                        "marketValue":        { "type": "number" },
                        "currency":           { "type": "string" },
                        "status":             { "type": "string", "enum": ["A", "C", "P"] },
                        "unrealizedGainLoss": { "type": "number" }
                    }
                },
                "Transaction": {
                    "type": "object",
                    "properties": {
                        "date":            { "type": "string", "format": "date", "nullable": true },
                        "portfolioId":     { "type": "string" },
                        "investmentId":    { "type": "string" },
                        "type":            { "type": "string", "enum": ["BU", "SL", "TR", "FE"] },
                        "quantity":        { "type": "number" },
                        "price":           { "type": "number" },
                        "amount":          { "type": "number" },
                        "currency":        { "type": "string" },
                        "status":          { "type": "string", "enum": ["P", "D", "F", "R"] },
                        "sequenceNo":      { "type": "string" },
                        "time":            { "type": "string", "format": "time", "nullable": true },
                        "processDate":     { "type": "string", "format": "date-time", "nullable": true },
                        "processUser":     { "type": "string" }
                    }
                },
                "Error": {
                    "type": "object",
                    "properties": {
                        "status":  { "type": "integer" },
                        "error":   { "type": "string" },
                        "message": { "type": "string" },
                        "details": {
                            "type": "array",
                            "nullable": true,
                            "items": {
                                "type": "object",
                                "properties": {
                                    "field":   { "type": "string" },
                                    "message": { "type": "string" }
                                }
                            }
                        }
                    }
                }
            }
        },
        "security": [{ "bearerAuth": [] }],
        "paths": {
            "/api/portfolios": {
                "get": {
                    "summary": "List portfolios",
                    "operationId": "listPortfolios",
                    "tags": ["portfolios"],
                    "parameters": [
                        { "name": "limit",  "in": "query", "schema": { "type": "integer", "default": 20 } },
                        { "name": "offset", "in": "query", "schema": { "type": "integer", "default": 0 } },
                        { "name": "status", "in": "query", "schema": { "type": "string" } }
                    ],
                    "responses": {
                        "200": { "description": "Paginated list of portfolios" },
                        "401": { "description": "Unauthorized" }
                    }
                },
                "post": {
                    "summary": "Create portfolio",
                    "operationId": "createPortfolio",
                    "tags": ["portfolios"],
                    "requestBody": {
                        "required": true,
                        "content": { "application/json": { "schema": { "$ref": "#/components/schemas/CreatePortfolio" } } }
                    },
                    "responses": {
                        "201": { "description": "Portfolio created" },
                        "400": { "description": "Validation error" },
                        "401": { "description": "Unauthorized" }
                    }
                }
            },
            "/api/portfolios/{id}": {
                "parameters": [
                    { "name": "id", "in": "path", "required": true, "schema": { "type": "string" } }
                ],
                "get": {
                    "summary": "Get portfolio by ID",
                    "operationId": "getPortfolio",
                    "tags": ["portfolios"],
                    "responses": {
                        "200": { "description": "Portfolio detail" },
                        "404": { "description": "Not found" }
                    }
                },
                "put": {
                    "summary": "Update portfolio",
                    "operationId": "updatePortfolio",
                    "tags": ["portfolios"],
                    "requestBody": {
                        "required": true,
                        "content": { "application/json": { "schema": { "$ref": "#/components/schemas/UpdatePortfolio" } } }
                    },
                    "responses": {
                        "200": { "description": "Updated portfolio" },
                        "404": { "description": "Not found" }
                    }
                },
                "delete": {
                    "summary": "Delete portfolio",
                    "operationId": "deletePortfolio",
                    "tags": ["portfolios"],
                    "responses": {
                        "204": { "description": "Deleted" },
                        "404": { "description": "Not found" }
                    }
                }
            },
            "/api/portfolios/{id}/positions": {
                "parameters": [
                    { "name": "id", "in": "path", "required": true, "schema": { "type": "string" } }
                ],
                "get": {
                    "summary": "List positions for a portfolio",
                    "operationId": "listPositions",
                    "tags": ["positions"],
                    "parameters": [
                        { "name": "limit",  "in": "query", "schema": { "type": "integer", "default": 20 } },
                        { "name": "offset", "in": "query", "schema": { "type": "integer", "default": 0 } }
                    ],
                    "responses": {
                        "200": { "description": "Paginated list of positions" },
                        "404": { "description": "Portfolio not found" }
                    }
                }
            },
            "/api/portfolios/{id}/transactions": {
                "parameters": [
                    { "name": "id", "in": "path", "required": true, "schema": { "type": "string" } }
                ],
                "get": {
                    "summary": "List transactions for a portfolio",
                    "operationId": "listTransactions",
                    "tags": ["transactions"],
                    "parameters": [
                        { "name": "limit",  "in": "query", "schema": { "type": "integer", "default": 20 } },
                        { "name": "offset", "in": "query", "schema": { "type": "integer", "default": 0 } }
                    ],
                    "responses": {
                        "200": { "description": "Paginated list of transactions" },
                        "404": { "description": "Portfolio not found" }
                    }
                }
            },
            "/health": {
                "get": {
                    "summary": "Health check",
                    "operationId": "healthCheck",
                    "tags": ["system"],
                    "security": [],
                    "responses": {
                        "200": { "description": "Service is healthy" }
                    }
                }
            }
        }
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn spec_is_valid_json() {
        let s = spec();
        assert_eq!(s["openapi"], "3.0.3");
        assert!(s["paths"]["/api/portfolios"]["get"].is_object());
        assert!(s["paths"]["/health"]["get"].is_object());
    }
}
