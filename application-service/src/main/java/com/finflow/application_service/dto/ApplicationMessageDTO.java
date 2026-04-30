package com.finflow.application_service.dto;

public class ApplicationMessageDTO {
    private Long id;
    private String name;
    private String applicantName;
    private Double amount;
    private String loanType;
    private Integer tenureMonths;
    private String status;
    private String action;

    public ApplicationMessageDTO() {
    }

    private ApplicationMessageDTO(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.applicantName = builder.applicantName;
        this.amount = builder.amount;
        this.loanType = builder.loanType;
        this.tenureMonths = builder.tenureMonths;
        this.status = builder.status;
        this.action = builder.action;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getLoanType() {
        return loanType;
    }

    public void setLoanType(String loanType) {
        this.loanType = loanType;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(Integer tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public static class Builder {
        private Long id;
        private String name;
        private String applicantName;
        private Double amount;
        private String loanType;
        private Integer tenureMonths;
        private String status;
        private String action;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder applicantName(String applicantName) {
            this.applicantName = applicantName;
            return this;
        }

        public Builder amount(Double amount) {
            this.amount = amount;
            return this;
        }

        public Builder loanType(String loanType) {
            this.loanType = loanType;
            return this;
        }

        public Builder tenureMonths(Integer tenureMonths) {
            this.tenureMonths = tenureMonths;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public ApplicationMessageDTO build() {
            return new ApplicationMessageDTO(this);
        }
    }
}
