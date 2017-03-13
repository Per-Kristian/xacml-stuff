package com.sun.xacml;

import com.sun.xacml.attr.AnyURIAttribute;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by piddy on 3/13/17.
 */
public class NilsenPEP {

    private PDP pdp = null;

    public NilsenPEP(String policy){

        FilePolicyModule policyModule = new FilePolicyModule();
        policyModule.addPolicy("policy/" + policy + ".xml");

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
        RFC822NameAttribute value =
                new RFC822NameAttribute(subjectName);

        // create the subject section with two attributes, the first with
        // the subject's identity...
        attributes.add(new Attribute(subjectId, null, null, value));
        // ...and the second with the subject's group membership
        attributes.add(new Attribute(new URI("group"),
                "admin@users.example.com", null,
                new StringAttribute("developers")));

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


    public static void main(String args[]){

        NilsenPEP nilsenPEP = null;
        String requestFile = null;

        requestFile = args[0];

        //String [] policyFiles = new String[args.length - 1];

        /*for (int i = 1; i < args.length; i++)
            policyFiles[i-1] = args[i];*/

        // create the new Request...note that the Environment must be specified
        // using a valid Set, even if that Set is empty
        try{
            RequestCtx request =
                    new RequestCtx(setupSubjects("BaselKatt"), setupResource("eval.docx"),
                            setupAction("read"), new HashSet());

            // encode the Request and print it to standard out
            request.encode(System.out, new Indenter());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // evaluate the request
        //ResponseCtx response = nilsenPEP.evaluate(requestFile);
    }
}
