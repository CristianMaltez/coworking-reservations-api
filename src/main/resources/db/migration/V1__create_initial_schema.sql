CREATE TABLE roles (
    id SMALLINT GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_code UNIQUE (code)
);


CREATE TABLE users (
    id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),

    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX uq_users_email_lower
    ON users (LOWER(email));


CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id SMALLINT NOT NULL,

    CONSTRAINT pk_user_roles
        PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles (id)
);

CREATE INDEX idx_user_roles_role_id
    ON user_roles (role_id);


CREATE TABLE space_types (
    id SMALLINT GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_space_types PRIMARY KEY (id),
    CONSTRAINT uq_space_types_code UNIQUE (code)
);


CREATE TABLE spaces (
    id UUID NOT NULL,
    space_type_id SMALLINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    capacity INTEGER NOT NULL,
    location VARCHAR(255) NOT NULL,
    hourly_rate NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_spaces PRIMARY KEY (id),

    CONSTRAINT fk_spaces_space_type
        FOREIGN KEY (space_type_id)
        REFERENCES space_types (id),

    CONSTRAINT chk_spaces_capacity
        CHECK (capacity > 0),

    CONSTRAINT chk_spaces_hourly_rate
        CHECK (hourly_rate >= 0)
);

CREATE INDEX idx_spaces_space_type_id
    ON spaces (space_type_id);


CREATE TABLE reservations (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    space_id UUID NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status SMALLINT NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_reservations PRIMARY KEY (id),

    CONSTRAINT fk_reservations_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT fk_reservations_space
        FOREIGN KEY (space_id)
        REFERENCES spaces (id),

    CONSTRAINT chk_reservations_status
        CHECK (
            status IN (
                1,
                2,
                3
            )
        ),

    CONSTRAINT chk_reservations_time_range
        CHECK (start_time < end_time),

    CONSTRAINT chk_reservations_total_price
        CHECK (total_price >= 0)
);

CREATE INDEX idx_reservations_user_start_time
    ON reservations (user_id, start_time);

CREATE INDEX idx_reservations_space_time_range
    ON reservations (space_id, start_time, end_time);
