import com.sun.xacml.*;
import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.RFC822NameAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Subject;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.impl.CurrentEnvModule;
import com.sun.xacml.finder.impl.FilePolicyModule;
import jdk.internal.org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Node;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * This is a very simple implementation of a PEP/PDP in XACML. It currently works with smaller policies like the
 * Bell-LaPadula security model.
 */
public class NilsenPEP {

    private PDP pdp = null;

    /**
     * This constructor takes one or more policies and initiates a new PDP object for later use.
     * @param policies one or more policy files
     */
    public NilsenPEP(String[] policies){

        FilePolicyModule policyModule = new FilePolicyModule();
        for (String policy : policies) {
            policyModule.addPolicy("policy/" + policy);
        }

        CurrentEnvModule envModule = new CurrentEnvModule();

        PolicyFinder policyFinder = new PolicyFinder();
        Set policyModules = new HashSet();
        policyModules.add(policyModule);
        policyFinder.setModules(policyModules);

        AttributeFinder attrFinder = new AttributeFinder();
        List attrModules = new ArrayList();
        attrModules.add(envModule);
        attrFinder.setModules(attrModules);

        pdp = new PDP(new PDPConfig(attrFinder, policyFinder, null));

    }


    /**
     * Sets up the Subject section of the request. This Request only has
     * one Subject section, and it uses the default category. To create a
     * Subject with a different category, you simply specify the category
     * when you construct the Subject object.
     *
     * @return a Set of Subject instances for inclusion in a Request
     *
     * @throws URISyntaxException if there is a problem with a URI
     */
    public static Set setupSubjects(String subjectName) throws URISyntaxException {
        HashSet attributes = new HashSet();

        // setup the id and value for the requesting subject
        URI subjectId =
                new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id");

        AttributeValue value = new StringAttribute(subjectName);
        // create the subject section with two attributes, the first with
        // the subject's identity...
        attributes.add(new Attribute(subjectId, null, null, value));
        // ...and the second with the subject's group membership
        /*attributes.add(new Attribute(new URI("group"),
                "admin@users.example.com", null,
                new StringAttribute("developers")));
                */

        // bundle the attributes in a Subject with the default category
        HashSet subjects = new HashSet();
        subjects.add(new Subject(attributes));

        return subjects;
    }

    /**
     * Creates a Resource specifying the resource-id, a required attribute.
     *
     * @return a Set of Attributes for inclusion in a Request
     *
     * @throws URISyntaxException if there is a problem with a URI
     */
    public static Set setupResource(String resourceName) throws URISyntaxException {
        HashSet resource = new HashSet();

        // the resource being requested
        AnyURIAttribute value =
                new AnyURIAttribute(new URI(resourceName));

        // create the resource using a standard, required identifier for
        // the resource being requested
        resource.add(new Attribute(new URI(EvaluationCtx.RESOURCE_ID),
                null, null, value));

        return resource;
    }

    /**
     * Creates an Action specifying the action-id, an optional attribute.
     *
     * @return a Set of Attributes for inclusion in a Request
     *
     * @throws URISyntaxException if there is a problem with a URI
     */
    public static Set setupAction(String actionName) throws URISyntaxException {
        HashSet action = new HashSet();

        // this is a standard URI that can optionally be used to specify
        // the action being requested
        URI actionId =
                new URI("urn:oasis:names:tc:xacml:1.0:action:action-id");

        // create the action
        action.add(new Attribute(actionId, null, null,
                new StringAttribute(actionName)));

        return action;
    }


    /**
     * Takes the path to the request XML file, reads it, and lets the PDP evaluate it.
     * @param requestFile The path to the request file
     * @return the result encoded as a string.
     */
    private String evaluate(String requestFile) {
        String requestPath = "request/" + requestFile;
        String results;
        ByteArrayOutputStream out;
        try {
            RequestCtx request =
                    RequestCtx.getInstance(new FileInputStream(requestPath));
            ResponseCtx response = pdp.evaluate(request);
            out = new ByteArrayOutputStream();


            response.encode(out, new Indenter());
            results = out.toString();
            /*
            resInput= new InputSource(new StringReader(results));

            XPath xPath = XPathFactory.newInstance().newXPath();
            String decision = xPath.evaluate("//Decision", resInput);
            System.out.println("Decision: " + decision);
            */
            //if (results.)
            return results;
            //return response;
        } catch(ParsingException|IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Evaluates the generated RequestCtx object, and returns a response from the PDP.
     * @param request the generated RequestCtx.
     * @return
     */
    private String evaluate(RequestCtx request) {
        ResponseCtx response = pdp.evaluate(request);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        response.encode(baos, new Indenter());
        return baos.toString();
    }

    /**
     * The main program. Depending on the arguments, it will either take a request and a policy, or subject + action +
     * resource + policy.
     * Only the '-config' option works at this time.
     * @param args either "-config resource.xml policy.xml" or "subject action resource policy.xml"
     */
    public static void main(String args[]){

        NilsenPEP nilsenPEP;
        String[] policies;

        try {
            if (args.length == 0) {
                System.out.println("Expecting arguments: <subject> <action> <resource> <policy.xml> [more policies]");
                System.out.println("Another option is to provide your own request file.");
                System.out.println("In that case, use -config <request.xml> <policy.xml> [more policies]");
                System.exit(1);
            } else if (args[0].equals("-config")) {
                policies = new String[args.length - 2];
                for(int i = 0; i <= args.length - 3; i++) {
                    policies[i] = args[i+2];
                }
                nilsenPEP = new NilsenPEP(policies);
                String result = nilsenPEP.evaluate(args[1]);
                System.out.print(result);
            }
            else {
                policies = new String[args.length - 3];
                for(int i = 0; i <= args.length - 4; i++) {
                    policies[i] = args[i+3];
                }
                nilsenPEP = new NilsenPEP(policies);

                RequestCtx request = new RequestCtx(setupSubjects(args[0]), setupAction(args[1]),
                        setupResource(args[2]), new HashSet());
                String result = nilsenPEP.evaluate(request);
                System.out.print(result);

            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
