CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    external_id UUID NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    second_last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    credit_line_amount NUMERIC(15, 2) NOT NULL,
    available_credit_line_amount NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_external_id ON customers(external_id);

CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,
    external_id UUID NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    amount NUMERIC(15, 2) NOT NULL,
    commission_amount NUMERIC(15, 2) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    scheme_name VARCHAR(20) NOT NULL,
    interest_rate NUMERIC(5, 4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_loans_external_id ON loans(external_id);
CREATE INDEX idx_loans_customer_id ON loans(customer_id);

CREATE TABLE installments (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
    installment_number INT NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    scheduled_payment_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_installments_loan_number UNIQUE (loan_id, installment_number)
);

CREATE INDEX idx_installments_loan_id ON installments(loan_id);
