# Payment-Integration

1. Payment service receives orders from other services by kafka events
2. Payment service create hosted payment request to third party payment gateways (such as noon)
3. Payment init the purchase order entity with payment link to be used later by client for payment


Spring-boot, integration with Noon payment and TELR payment using hosted payment APIS, Kafka consumer and producer used to communicate with other services
