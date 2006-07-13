/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.searchpolicy.*;
import org.apache.felix.moduleloader.IModule;

public class Util
{
    /**
     * Converts a module identifier to a bundle identifier. Module IDs
     * are typically <tt>&lt;bundle-id&gt;.&lt;revision&gt;</tt>; this
     * method returns only the portion corresponding to the bundle ID.
    **/
    public static long getBundleIdFromModuleId(String id)
    {
        try
        {
            String bundleId = (id.indexOf('.') >= 0)
                ? id.substring(0, id.indexOf('.')) : id;
            return Long.parseLong(bundleId);
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }
    }

    /**
     * Converts a module identifier to a bundle identifier. Module IDs
     * are typically <tt>&lt;bundle-id&gt;.&lt;revision&gt;</tt>; this
     * method returns only the portion corresponding to the revision.
    **/
    public static int getModuleRevisionFromModuleId(String id)
    {
        try
        {
            String rev = (id.indexOf('.') >= 0)
                ? id.substring(id.indexOf('.') + 1) : id;
            return Integer.parseInt(rev);
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }
    }

    public static String getClassName(String className)
    {
        if (className == null)
        {
            className = "";
        }
        return (className.lastIndexOf('.') < 0)
            ? "" : className.substring(className.lastIndexOf('.') + 1);
    }

    public static String getClassPackage(String className)
    {
        if (className == null)
        {
            className = "";
        }
        return (className.lastIndexOf('.') < 0)
            ? "" : className.substring(0, className.lastIndexOf('.'));
    }

    public static String getResourcePackage(String resource)
    {
        if (resource == null)
        {
            resource = "";
        }
        // NOTE: The package of a resource is tricky to determine since
        // resources do not follow the same naming conventions as classes.
        // This code is pessimistic and assumes that the package of a
        // resource is everything up to the last '/' character. By making
        // this choice, it will not be possible to load resources from
        // imports using relative resource names. For example, if a
        // bundle exports "foo" and an importer of "foo" tries to load
        // "/foo/bar/myresource.txt", this will not be found in the exporter
        // because the following algorithm assumes the package name is
        // "foo.bar", not just "foo". This only affects imported resources,
        // local resources will work as expected.
        String pkgName = (resource.startsWith("/")) ? resource.substring(1) : resource;
        pkgName = (pkgName.lastIndexOf('/') < 0)
            ? "" : pkgName.substring(0, pkgName.lastIndexOf('/'));
        pkgName = pkgName.replace('/', '.');
        return pkgName;
    }

    /**
     * <p>
     * This is a simple utility class that attempts to load the named
     * class using the class loader of the supplied class or
     * the class loader of one of its super classes or their implemented
     * interfaces. This is necessary during service registration to test
     * whether a given service object implements its declared service
     * interfaces.
     * </p>
     * <p>
     * To perform this test, the framework must try to load
     * the classes associated with the declared service interfaces, so
     * it must choose a class loader. The class loader of the registering
     * bundle cannot be used, since this disallows third parties to
     * register service on behalf of another bundle. Consequently, the
     * class loader of the service object must be used. However, this is
     * also not sufficient since the class loader of the service object
     * may not have direct access to the class in question.
     * </p>
     * <p>
     * The service object's class loader may not have direct access to
     * its service interface if it extends a super class from another
     * bundle which implements the service interface from an imported
     * bundle or if it implements an extension of the service interface
     * from another bundle which imports the base interface from another
     * bundle. In these cases, the service object's class loader only has
     * access to the super class's class or the extended service interface,
     * respectively, but not to the actual service interface.
     * </p>
     * <p>
     * Thus, it is necessary to not only try to load the service interface
     * class from the service object's class loader, but from the class
     * loaders of any interfaces it implements and the class loaders of
     * all super classes.
     * </p>
     * @param svcObj the class that is the root of the search.
     * @param name the name of the class to load.
     * @return the loaded class or <tt>null</tt> if it could not be
     *         loaded.
    **/
    public static Class loadClassUsingClass(Class clazz, String name)
    {
        Class loadedClass = null;
    
        while (clazz != null)
        {
            // Get the class loader of the current class object.
            ClassLoader loader = clazz.getClassLoader();
            // A null class loader represents the system class loader.
            loader = (loader == null) ? ClassLoader.getSystemClassLoader() : loader;
            try
            {
                return loader.loadClass(name);
            }
            catch (ClassNotFoundException ex)
            {
                // Ignore and try interface class loaders.
            }
    
            // Try to see if we can load the class from
            // one of the class's implemented interface
            // class loaders.
            Class[] ifcs = clazz.getInterfaces();
            for (int i = 0; i < ifcs.length; i++)
            {
                loadedClass = loadClassUsingClass(ifcs[i], name);
                if (loadedClass != null)
                {
                    return loadedClass;
                }
            }
    
            // Try to see if we can load the class from
            // the super class class loader.
            clazz = clazz.getSuperclass();
        }
    
        return null;
    }

    public static R4Export getExportPackage(IModule m, String name)
    {
        R4Export[] pkgs =
            ((IR4SearchPolicy) m.getContentLoader().getSearchPolicy()).getExports();
        for (int i = 0; (pkgs != null) && (i < pkgs.length); i++)
        {
            if (pkgs[i].getName().equals(name))
            {
                return pkgs[i];
            }
        }
        return null;
    }

    public static R4Import getImportPackage(IModule m, String name)
    {
        R4Import[] pkgs =
            ((IR4SearchPolicy) m.getContentLoader().getSearchPolicy()).getImports();
        for (int i = 0; (pkgs != null) && (i < pkgs.length); i++)
        {
            if (pkgs[i].getName().equals(name))
            {
                return pkgs[i];
            }
        }
        return null;
    }

    public static R4Wire getWire(IModule m, String name)
    {
        R4Wire[] wires =
            ((IR4SearchPolicy) m.getContentLoader().getSearchPolicy()).getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            if (wires[i].getExport().getName().equals(name))
            {
                return wires[i];
            }
        }
        return null;
    }

    /**
     * Parses delimited string and returns an array containing the tokens. This
     * parser obeys quotes, so the delimiter character will be ignored if it is
     * inside of a quote. This method assumes that the quote character is not
     * included in the set of delimiter characters.
     * @param value the delimited string to parse.
     * @param delim the characters delimiting the tokens.
     * @return an array of string tokens or null if there were no tokens.
    **/
    public static String[] parseDelimitedString(String value, String delim)
    {
        if (value == null)
        {
           value = "";
        }

        List list = new ArrayList();

        int CHAR = 1;
        int DELIMITER = 2;
        int STARTQUOTE = 4;
        int ENDQUOTE = 8;

        StringBuffer sb = new StringBuffer();

        int expecting = (CHAR | DELIMITER | STARTQUOTE);
        
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);

            boolean isDelimiter = (delim.indexOf(c) >= 0);
            boolean isQuote = (c == '"');

            if (isDelimiter && ((expecting & DELIMITER) > 0))
            {
                list.add(sb.toString().trim());
                sb.delete(0, sb.length());
                expecting = (CHAR | DELIMITER | STARTQUOTE);
            }
            else if (isQuote && ((expecting & STARTQUOTE) > 0))
            {
                sb.append(c);
                expecting = CHAR | ENDQUOTE;
            }
            else if (isQuote && ((expecting & ENDQUOTE) > 0))
            {
                sb.append(c);
                expecting = (CHAR | STARTQUOTE | DELIMITER);
            }
            else if ((expecting & CHAR) > 0)
            {
                sb.append(c);
            }
            else
            {
                throw new IllegalArgumentException("Invalid delimited string: " + value);
            }
        }

        if (sb.length() > 0)
        {
            list.add(sb.toString().trim());
        }

        return (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * Parses native code manifest headers.
     * @param libStrs an array of native library manifest header
     *        strings from the bundle manifest.
     * @return an array of <tt>LibraryInfo</tt> objects for the
     *         passed in strings.
    **/
    public static R4LibraryHeader[] parseLibraryStrings(Logger logger, String[] libStrs)
        throws IllegalArgumentException
    {
        if (libStrs == null)
        {
            return new R4LibraryHeader[0];
        }

        List libList = new ArrayList();

        for (int i = 0; i < libStrs.length; i++)
        {
            R4LibraryHeader[] libs = R4LibraryHeader.parse(logger, libStrs[i]);
            for (int libIdx = 0;
                (libs != null) && (libIdx < libs.length);
                libIdx++)
            {
                libList.add(libs[libIdx]);
            }
        }

        return (R4LibraryHeader[]) libList.toArray(new R4LibraryHeader[libList.size()]);
    }

    private static final byte encTab[] = { 0x41, 0x42, 0x43, 0x44, 0x45, 0x46,
        0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51, 0x52,
        0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x61, 0x62, 0x63, 0x64,
        0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f, 0x70,
        0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x30, 0x31,
        0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x2b, 0x2f };

    private static final byte decTab[] = { -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1,
        -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1,
        -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29,
        30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
        48, 49, 50, 51, -1, -1, -1, -1, -1 };

    public static String base64Encode(String s) throws IOException
    {
        return encode(s.getBytes(), 0);
    }

    /**
     * Encode a raw byte array to a Base64 String.
     * 
     * @param in Byte array to encode.
     * @param len Length of Base64 lines. 0 means no line breaks.
    **/
    public static String encode(byte[] in, int len) throws IOException
    {
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try
        {
            baos = new ByteArrayOutputStream();
            bais = new ByteArrayInputStream(in);
            encode(bais, baos, len);
            // ASCII byte array to String
            return (new String(baos.toByteArray()));
        }
        finally
        {
            if (baos != null)
            {
                baos.close();
            }
            if (bais != null)
            {
                bais.close();
            }
        }
    }

    public static void encode(InputStream in, OutputStream out, int len)
        throws IOException
    {

        // Check that length is a multiple of 4 bytes
        if (len % 4 != 0)
        {
            throw new IllegalArgumentException("Length must be a multiple of 4");
        }

        // Read input stream until end of file
        int bits = 0;
        int nbits = 0;
        int nbytes = 0;
        int b;

        while ((b = in.read()) != -1)
        {
            bits = (bits << 8) | b;
            nbits += 8;
            while (nbits >= 6)
            {
                nbits -= 6;
                out.write(encTab[0x3f & (bits >> nbits)]);
                nbytes++;
                // New line
                if (len != 0 && nbytes >= len)
                {
                    out.write(0x0d);
                    out.write(0x0a);
                    nbytes -= len;
                }
            }
        }

        switch (nbits)
        {
            case 2:
                out.write(encTab[0x3f & (bits << 4)]);
                out.write(0x3d); // 0x3d = '='
                out.write(0x3d);
                break;
            case 4:
                out.write(encTab[0x3f & (bits << 2)]);
                out.write(0x3d);
                break;
        }

        if (len != 0)
        {
            if (nbytes != 0)
            {
                out.write(0x0d);
                out.write(0x0a);
            }
            out.write(0x0d);
            out.write(0x0a);
        }
    }
}