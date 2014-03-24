package org.jboss.resteasy.test.regression;

import static org.jboss.resteasy.test.TestPortProvider.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * RESTEASY-573
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LazyInitUriInfoInjectionTest extends BaseResourceTest
{
   @Path("/test")
   public static class MyTest
   {
      private UriInfo info;

      @Context
      public void setUriInfo(UriInfo i)
      {
         this.info = i;
         System.out.println(i.getClass().getName());
      }

      @GET
      @Produces("text/plain")
      public String get()
      {
         String val = info.getQueryParameters().getFirst("h");
         if (val == null) val = "";
         return val;
      }


   }

   public static class LazySingletonResource implements ResourceFactory
   {
      private final Class<?> clazz;
      private InjectorFactory factory;
      private Object obj;

      public LazySingletonResource(Class<?> clazz)
      {
         this.clazz = clazz;
      }

      @Override
    public void registered(InjectorFactory factory)
      {
         this.factory = factory;
      }

      @Override
    public Object createResource(HttpRequest request, HttpResponse response, InjectorFactory factory)
      {
         if (obj == null)
         {
            try
            {
               obj = clazz.newInstance();
            }
            catch (InstantiationException e)
            {
               throw new RuntimeException(e);
            }
            catch (IllegalAccessException e)
            {
               throw new RuntimeException(e);
            }
            this.factory.createPropertyInjector(clazz).inject(obj);
         }
         return obj;
      }

      @Override
    public void unregistered()
      {
      }

      @Override
    public Class<?> getScannableClass()
      {
         return clazz;
      }

      @Override
    public void requestFinished(HttpRequest request, HttpResponse response, Object resource)
      {
      }
   }


   @Override
   @Before
   public void before() throws Exception {
      super.before();
      dispatcher.getRegistry().addResourceFactory(new LazySingletonResource(MyTest.class));
   }

   @Test
   public void testDup() throws Exception
   {
      ClientRequest request = new ClientRequest(generateURL("/test"));
      request.queryParameter("h", "world");
      String val = request.getTarget(String.class);
      Assert.assertEquals(val, "world");

      request = new ClientRequest(generateURL("/test"));
      val = request.getTarget(String.class);
      Assert.assertEquals(val, "");


   }

}
