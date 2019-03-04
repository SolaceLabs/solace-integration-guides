#### Creating a Message VPN

If you are using Solace Cloud you can skip this step because a message-VPN is already assigned. For the name, refer to the "Message VPN" in the connection details page.

This section outlines how to create a message-VPN called "{{ include.content }}" on the message broker with authentication disabled and 2GB of message spool quota for Guaranteed Messaging. This message-VPN name is required in the Spring configuration when connecting to the messaging message broker. In practice, appropriate values for authentication, message spool and other message-VPN properties should be chosen depending on the end application’s use case.



```
> home
> enable
# configure
(config)# create message-vpn {{ include.content }}
(config-msg-vpn)# authentication
(config-msg-vpn-auth)# user-class client
(config-msg-vpn-auth-user-class)# basic auth-type none
(config-msg-vpn-auth-user-class)# exit
(config-msg-vpn-auth)# exit
(config-msg-vpn)# no shutdown
(config-msg-vpn)# exit
(config)#
(config)# message-spool message-vpn {{ include.content }}
(config-message-spool)# max-spool-usage 2000
(config-message-spool)# exit
(config)#
```
