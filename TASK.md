Please implement a small REST API containing one method:
- POST api/authorizations

The method should be able to process a request from a third party card issuer. The card issuer on the basis of REST API response will authorize or decline a payment transaction in a card terminal.

In a scope of the task you can decide how the request should look like. Let assume that the card issuer is able to provide any data and serve them in any format you specify.

However, in response card issuer expects to receive json with following structure:
{
"is_authorized" true/false,
"description" "optional - only present if is_authorized = false, contains reason why authorization has failed"  
}

The authorization service should store account balances. To persistist an account balance use a database system which should support database transactions. An authorization request should be able modify one or more account balances. However there should be some simple business rules that do not let every payment transaction pass authorization e.g. if payment transaction makes at least one account balance negative, it is declined.

When a payment transaction attempts to operate on any account, the account owner should be notified about it. The account owner should be informed about successful transactions but as well about declined transactions referring to his or her account. Let assume that account notifications are delivered via some third party service which does not provide a transactional API, only a non-transactional, file API. Let assume that if our system wants to send a notification, it has to write it to a file and store this file in the directory called "ready-for-dispatch". The third party service is observing the directory and after it dispatches a notification, it moves the file to the "sent" directory.

Please design a solution where after REST API request is received, 3 following actions are synchronized/orchestrated in a way that the flow is predictable and the service state is consistent:
- specific account balances are updated, when transaction is successful,
- response is delivered to calling service via REST API,
- notification is dispatched to the account owner.

Suggested technologies: ring, reitit, postgres (suggested but not required, can be any other database).

Please provide tests showing different scenarios. Present at least one scenario where database transaction is broken before it is committed e.g. by programmatically dropping database connection, to show what happens with notification and REST API response in such a case.
