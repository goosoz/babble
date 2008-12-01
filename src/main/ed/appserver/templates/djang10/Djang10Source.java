/**
*    Copyright (C) 2008 10gen Inc.
*
*    This program is free software: you can redistribute it and/or  modify
*    it under the terms of the GNU Affero General Public License, version 3,
*    as published by the Free Software Foundation.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU Affero General Public License for more details.
*
*    You should have received a copy of the GNU Affero General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package ed.appserver.templates.djang10;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import ed.appserver.AppContext;
import ed.appserver.JSFileLibrary;
import ed.appserver.jxp.JxpSource;
import ed.appserver.templates.djang10.JSHelper.LoadedLibrary;
import ed.io.StreamUtil;
import ed.js.JSArray;
import ed.js.JSFunction;
import ed.js.JSObject;
import ed.js.JSObjectSize;
import ed.js.engine.JSCompiledScript;
import ed.js.engine.Scope;
import ed.lang.StackTraceHolder;
import ed.log.Logger;
import ed.util.Dependency;
import ed.util.Pair;
import ed.util.SeenPath;
import ed.util.Sizable;

public class Djang10Source extends JxpSource implements Sizable {
    private final Logger log = Logger.getRoot().getChild("djang10").getChild("Djang10Source");

    private final Djang10Content content;
    private Djang10CompiledScript compiledScript;
    private final Scope scope;

    public Djang10Source(Scope scope, File f) {
        content = new Djang10File(f);
        compiledScript = null;
        this.scope = scope;
    }
    public Djang10Source(Scope scope, String content) {
        this.content = new DJang10String(content);
        compiledScript = null;
        this.scope = scope;
    }

    public JSFunction getFunction() throws IOException {
        try {
            return _getFunction();
        } catch(RuntimeException e) {
            StackTraceHolder.getInstance().fix(e);
            Djang10CompiledScript.fix(e);

            throw e;
        }
    }
    public synchronized JSFunction _getFunction() throws IOException {
        if(_needsParsing() || compiledScript == null) {
            log.debug("Parsing " + content.getDebugName());

            compiledScript = null;
            _lastParse = lastUpdated();
            _dependencies.clear();

            NodeList nodes = null;
            Collection<Library> libraries;

            String contents = getContent();


            Parser parser = new Parser(scope, content.getName(), contents);
            JSHelper jsHelper = JSHelper.get(scope);

            for(LoadedLibrary lib : jsHelper.getDefaultLibraries()) {
                parser.add_dependency(lib.getSource());
                parser.add_library(lib.getLibrary());
            }

            nodes = parser.parse(new JSArray());
            libraries = parser.getLoadedLibraries();

            _dependencies.addAll(parser.get_dependencies());

            compiledScript = new Djang10CompiledScript(nodes, libraries);
            compiledScript.set(JxpSource.JXP_SOURCE_PROP, this);

            log.debug("Done Parsing " + content.getDebugName());
        }

        return compiledScript;
    }

    public File getFile() {
        return (content instanceof Djang10File)? ((Djang10File)content).getFile() : null;
    }
    public long lastUpdated(Set<Dependency> visitedDeps) {
        visitedDeps.add(this);

        long lastUpdated = content.lastUpdated();
        for(Dependency dep : _dependencies)
            if(!visitedDeps.contains(dep))
                lastUpdated = Math.max(lastUpdated, dep.lastUpdated(visitedDeps));

        return lastUpdated;
    }
    protected String getContent() throws IOException {
        return content.getContent();
    }
    protected InputStream getInputStream() throws IOException {
        return content.getInputStream();
    }
    public String getName() {
        return content.getName();
    }

    public long approxSize(SeenPath seen) {
        long sum = super.approxSize( seen );

        Object[] toSize = { log, content, compiledScript, scope };
        for( Object obj : toSize )
            sum += JSObjectSize.size( obj, seen, this );

        return sum;
    }

    private static interface Djang10Content extends Sizable {
        public String getContent() throws IOException;
        public InputStream getInputStream() throws IOException;
        public long lastUpdated();
        public String getName();
        public String getDebugName();
    }
    private static class Djang10File implements Djang10Content {
        private final File file;

        public Djang10File(File file) {
            this.file = file;
        }
        public String getContent() throws IOException {
            return StreamUtil.readFully(file);
        }
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }
        public long lastUpdated() {
            return file.lastModified();
        }
        public File getFile() {
            return file;
        }
        public String getName() {
            return file.toString();
        }
        public String getDebugName() {
            return file.toString();
        }
        public long approxSize(SeenPath seen) {
            long sum = JSObjectSize.OBJ_OVERHEAD;

            sum += JSObjectSize.size( file );

            return sum;
        }
    }
    private static class DJang10String implements Djang10Content {
        private final String content;
        private final long timestamp;

        public DJang10String(String content) {
            this.content = content;
            timestamp = System.currentTimeMillis();
        }
        public String getContent() throws IOException {
            return content;
        }
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content.getBytes());
        }
        public long lastUpdated() {
            return timestamp;
        }
        public String getName() {
            return "temp"+timestamp+".djang10";
        }
        public String getDebugName() {
            return "String: " + content.replaceAll("\n", "\\n").replace("\t", "\\t");
        }

        public long approxSize(SeenPath seen) {
            long sum = JSObjectSize.OBJ_OVERHEAD;

            sum += JSObjectSize.size( content );
            sum += JSObjectSize.size( timestamp );

            return sum;
        }
    }
}
