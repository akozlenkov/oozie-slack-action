# oozie-slack-action

<action name="send">
    <slack xmlns="uri:oozie:slack-action:0.1">
        <text>Some text</text>
        <webhook-uri>Slack webhook uri</webhook-uri>
    </slack>
    <ok to="end"/>
    <error to="kill"/>
</action>
