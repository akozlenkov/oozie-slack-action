package ru.hh.oozie.action;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.oozie.action.ActionExecutor;
import org.apache.oozie.action.ActionExecutorException;
import org.apache.oozie.action.ActionExecutorException.ErrorType;
import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.util.XLog;
import org.apache.oozie.util.XmlUtils;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;

import org.apache.http.client.ClientProtocolException;

public class SlackActionExecutor extends ActionExecutor {
    private static final String SUCCEEDED = "OK";
    private static final String KILLED = "KILLED";

    private final static String TEXT = "text";
    private final static String WEBHOOK_URI = "webhook-uri";

    private final XLog LOG = XLog.getLog(getClass());

    public SlackActionExecutor() {
        super("slack");
    }

    @Override
    public void initActionType() {
        super.initActionType();
        registerError(JDOMException.class.getName(), ActionExecutorException.ErrorType.ERROR, "HES001");
        registerError(ClientProtocolException.class.getName(), ActionExecutorException.ErrorType.ERROR, "HES002");
    }

    @Override
    public void start(Context context, WorkflowAction workflowAction) throws ActionExecutorException {
        LOG.info("Starting action");

        try {
            context.setStartData("-", "-", "-");
            Element actionXml = XmlUtils.parseXml(workflowAction.getConf());
            validateAndSend(context, actionXml);
            context.setExecutionData(SUCCEEDED, null);
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            throw convertException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    protected void validateAndSend(Context context, Element element) throws ActionExecutorException {
        Namespace ns = element.getNamespace();

        String text = element.getChildTextTrim(TEXT, ns);
        if (text.isEmpty()) {
            throw new ActionExecutorException(ErrorType.ERROR, "SL001", "text is required.");
        }

        String webhookUri = element.getChildTextTrim(WEBHOOK_URI, ns);
        if (webhookUri.isEmpty()) {
            throw new ActionExecutorException(ErrorType.ERROR, "SL002", "webhook-uri is required.");
        }

        Gson gson = new Gson();

        try {
            CloseableHttpClient httpClient = HttpClients.custom().
                    setHostnameVerifier(new AllowAllHostnameVerifier()).
                    setSslcontext(new SSLContextBuilder().loadTrustMaterial(null, (arg0, arg1) -> true).build()).build();
            HttpPost post = new HttpPost(webhookUri);
            StringEntity postingString = new StringEntity(gson.toJson(new SlackMessage(text)));
            post.setEntity(postingString);
            post.setHeader("Content-type", "application/json");
            HttpResponse response = httpClient.execute(post);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new ActionExecutorException(ErrorType.ERROR, "SL004", "Bad status code");
            }
        } catch (Exception ex) {
            throw new ActionExecutorException(ErrorType.ERROR, "SL003", ex.getMessage(), ex);
        }

    }

    @Override
    public void end(Context context, WorkflowAction workflowAction) throws ActionExecutorException {
        String externalStatus = workflowAction.getExternalStatus();
        WorkflowAction.Status status = externalStatus.equals(SUCCEEDED) ? WorkflowAction.Status.OK : WorkflowAction.Status.ERROR;
        context.setEndData(status, getActionSignal(status));
        LOG.info("Action ended with external status [{0}]", workflowAction.getExternalStatus());
    }

    @Override
    public void check(Context context, WorkflowAction action) throws ActionExecutorException {
    }

    @Override
    public void kill(Context context, WorkflowAction workflowAction) throws ActionExecutorException {
        context.setExternalStatus(KILLED);
        context.setExecutionData(KILLED, null);
    }

    @Override
    public boolean isCompleted(String s) {
        return true;
    }
}
