var defaulttags =
    djang10.defaulttags = 
    {};

register = new djang10.Library();



var CommentNode =
    defaulttags.CommentNode =
    function() {
};

CommentNode.prototype = {
    __proto__: djang10.Node.prototype,
    
    toString: function() {
        return "<Comment Node>"
    },
    __render: function(context, printer) {
        //noop
    }
};


var CycleNode =
    defaulttags.CycleNode =
    function(cyclevars, variable_name) {
        
    this.cyclevars = cyclevars;
    this.i = 0;
    this.variable_name = variable_name;
};
CycleNode.prototype = {
    __proto__: djang10.Node.prototype,
    
    toString: function() {        
        return "<Cycle Node: cycleVars: " + this.cyclevars + ", name: " + this.variable_name + ">"; 
    },
    __render: function(context, printer) {
        var template_vars = context["__render_vars"];
        if(template_vars == null)
            template_vars = context["__render_vars"] = new Map();
        
        var i = template_vars.get(this) || 0;

        
        var value = this.cyclevars[i].resolve(context);

        if(++i >= this.cyclevars.length)
            i = 0;
        template_vars.set(this, i);

        if(this.variable_name)
            context[this.variable_name] = value;
        
        printer(value);
    }
};

var FilterNode =
    defaulttags.FilterNode =
    function(filter_expr, nodelist) {
        
        this.filter_expr = filter_expr;
        this.nodelist = nodelist;
};
FilterNode.prototype = {
    __proto__: djang10.Node.prototype,
    
    toString: function() {
        return "<Filter Node: " + filter_expr + ">";
    },
    
    __render: function(context, printer) {
        context.push();
        var output = this.nodelist.render(context);
        context["temp"] = output;
        var filtered = this.filter_expr.resolve(context);
        context.pop();
        
        printer(filtered);
    }
};

var FirstOfNode =
    defaulttags.FirstOfNode =
    function(exprs) {
        
    this.exprs = exprs;
};
FirstOfNode.prototype = {
    __proto__: djang10.Node.prototype,
    
    toString: function() {
        return "<FirstOf Node: " + this.exprs.join(",").substring(0, 25) + "...>";
    },
    
    __render: function(context, printer) {
        for(var i=0; i<this.exprs.length; i++) {
            var expr = this.exprs[i];
            var value = expr.resolve(context);
            if(djang10.Expression.is_true(value)) {
                printer(value);
                return;
            }
        }
    }
}

var ForNode =
    defaulttags.ForNode =
    function(loopvar, sequence, is_reversed, nodelist_loop) {
    
    this.loopvar = loopvar;
    this.sequence = sequence;
    this.is_reversed = is_reversed;
    this.nodelist_loop = nodelist_loop;
    
};

ForNode.prototype = {
    __proto__: djang10.Node.prototype,
    
    toString: function() {
        var rev_text = this.is_reversed? " reversed" : "";
        return "<For Node: for " + this.loopvar 
            + " in " + this.sequence 
            + ", tail_len: " + this.nodelist_loop.length 
            + rev_text + ">"; 
    },
    get_nodes_by_type: function(constructor) {
        var nodes = [];
        if(isinstance(this, constructor))
            nodes.push(this);
        
        nodes.addAll(this.nodelist_loop.get_nodes_by_type(constructor));
        
        return nodes;
    },
    __render: function(context, printer) {
        var parentloop;
        
        parentloop = ("forloop" in context)? parentloop = context["forloop"] : {};
        context.push();
        
        var values = this.sequence.resolve(context);
        if(!djang10.Expression.is_true(values))
            values = [];
        if(this.is_reversed)
            values.reverse();
        
        var loop_dict = context["forloop"] = {parentloop: parentloop};
        for(var i=0; i<values.length; i++) {
            var item = values[i];
            
            loop_dict['counter0'] = i;
            loop_dict['counter'] = i+1;
            loop_dict['revcounter'] = values.length - i;
            loop_dict['revcounter0'] = values.length - i - 1;
            loop_dict['first'] = (i==0);
            loop_dict['last'] = (i== (values.length -1));
            
            context[this.loopvar] = item;
            
            this.nodelist_loop.__render(context, printer);
        }
        context.pop();
    }
};

var IfChangedNode =
    defaulttags.IfChangedNode =
    function(nodelist, exprs) {

    this.nodelist = nodelist;
    this._varlist = exprs;    
};
IfChangedNode.are_equal = function(a, b){
    if(typeof(a) != typeof(b))
        return false;

    if(typeof(a) != "object" || typeof(b) != "object")
        return a == b;

    if((a instanceof Array) && (b instanceof Array)) {
        if(a.length != b.length) return false;
        for(var i=0; i<a.length; i++)
            if(!IfChangedNode.are_equal(a[i], b[i]))
                return false;
        return true; 
    }
        
    if(a.equals instanceof Function)
        return a.equals(b);
    
    if(b.equals instanceof Function)
        return b.equals(a);
    
    return a == b;
};
IfChangedNode.prototype = {
    __proto__: djang10.Node.prototype,
    
    toString: function() {
        return "<IfChanged Node: " + tojson(this.exprs) + ">";
    },
    __render: function(context, printer) {
        var template_vars = context["__render_vars"];
        if(template_vars == null)
            template_vars = context["__render_vars"] = new Map();

        var last_seen = (("forloop" in context) && context.forloop.first)? null : template_vars.get(this);
        
        var compare_to;
        var is_same;
        if(this._varlist.length > 0)
            compare_to = this._varlist.map(function(expr) { return expr.resolve(context); });
        else
            compare_to = this.nodelist.render(context);
            
        if(!IfChangedNode.are_equal(last_seen, compare_to)) {
            var firstloop = (last_seen == null);
            last_seen = compare_to;
            template_vars.set(this, last_seen);
            
            context.push();
            context["ifchanged"] = {"firstloop": firstloop};
            this.nodelist.__render(context, printer);
            context.pop();
        }
    }
};

var IfEqualNode =
    defaulttags.IfEqualNode =
    function(var1, var2, nodelist_true, nodelist_false, negate) {

    this.var1 = var1;
    this.var2 = var2;
    
    this.nodelist_true = nodelist_true;
    this.nodelist_false = nodelist_false;
    this.negate = negate;
};
IfEqualNode.prototype = {
    __proto__: djang10.Node.prototype,
    
    toString: function() {
        return "<IfEqualNode>";
    },
    
    __render: function(context, printer) {
        var value1 = this.var1.resolve(context);
        var value2 = this.var2.resolve(context);
        
        if(this.negate != IfChangedNode.are_equal(value1, value2)) {
            this.nodelist_true.__render(context, printer);
        }
        else {
            this.nodelist_false.__render(context, printer);
        }
    }
};


var IfNode =
    defaulttags.IfNode =
    function(bool_exprs, nodelist_true, nodelist_false, link_type) {

    this.bool_exprs = bool_exprs;
    this.nodelist_true = nodelist_true;
    this.nodelist_false = nodelist_false;
    this.link_type = link_type;
};
IfNode.LinkTypes = {
    and_: 0,
    or_: 1
};
IfNode.BoolExpr = function(ifnot, bool_expr) {
    this.ifnot = ifnot;
    this.bool_expr = bool_expr;
};
IfNode.BoolExpr.prototype = {
    toString: function() {
        return (this.ifnot?"not ": "") + this.bool_expr;
    }
};
IfNode.prototype = {
    __proto: djang10.Node.prototype,
    
    toString: function() {
        return "<If node: " + this.bool_exprs + ">";
    },
    
    get_nodes_by_type: function(constructor) {
        var nodes = [];
        if(isinstance(this, constructor))
            nodes.push(this);
        
        nodes.addAll(this.nodelist_true.get_nodes_by_type(constructor));
        nodes.addAll(this.nodelist_false.get_nodes_by_type(constructor));
        
        return nodes;
    },
    
    __render: function(context, printer) {
        if(this.link_type == IfNode.LinkTypes.or_) {

            for(var i=0; i<this.bool_exprs.length; i++) {
                var bool_expr = this.bool_exprs[i];

                var value = bool_expr.bool_expr.resolve(context);
                if(djang10.Expression.is_true(value) != bool_expr.ifnot)
                    return this.nodelist_true.__render(context, printer);
            }
            return this.nodelist_false.__render(context, printer);
        }
        else {
            for(var i=0; i<this.bool_exprs.length; i++) {
                var bool_expr = this.bool_exprs[i];
                var value = bool_expr.bool_expr.resolve(context);
                if(djang10.Expression.is_true(value) == bool_expr.ifnot)
                    return this.nodelist_false.__render(context, printer);
            }
            return this.nodelist_true.__render(context, printer);
        }
    }
};

var RegroupNode =
    defaulttags.RegroupNode =
    function(the_list, prop_name, var_name) {

    this.the_list = the_list;
    this.prop_name = prop_name;
    this.var_name = var_name;
};
RegroupNode.prototype = {
    __proto__: djang10.Node.prototype,
    
    toString: function() {
        return "<Regroup Node: " + this.the_list + " by " + this.prop_name + " as " + this.var_name +">";
    },
    __render: function(context, printer) {
        var obj_list = this.the_list.resolve(context);
        if(!djang10.Expression.is_true(obj_list)) {
            context[this.var_name] = [];
            return;
        }
        
        var grouped = [];
        var group = null;
        
        var prop_name = this.prop_name;
        
        if(prop_name) {
            obj_list.each(function(item){
                if (group == null || group.grouper != item[prop_name]) {
                    group = {
                        grouper: item[prop_name],
                        list: []
                    };
                    grouped.push(group);
                }
                group.list.push(item);
            });
        }
        context[this.var_name] = grouped;
    }
};

var LoadNode =
    defaulttags.LoadNode =
    function() {

};
LoadNode.prototype = {
    __proto__: djang10.Node.prototype,
    
    toString: function() {
        return "<Load Node>";
    },
    __render: function(context, printer) {
    }
};

var NowNode =
    defaulttags.NodeNode =
    function(format_expr) {

    this.format_expr = format_expr;
};
NowNode.prototype = {
    __proto__: djang10.Node.prototype,
    
    toString: function() {
        return "<Now Node: " + this.format_Expr + ">";
    },
    __render: function(context, printer) {
        var format = this.format_expr.resolve(context);
        var formatted_date = djang10.formatDate(new Date(), format);
        printer(formatted_date);
    }
};

var SpacelessNode =
    defaulttags.SpacelessNode =
    function(nodelist) {

    this.nodelist = nodelist;
};
SpacelessNode.prototype ={
    __proto__: djang10.Node.prototype,
    
    toString: function() {
        return "<Spaceless Node>";
    },
    __render: function(context, printer) {
        var content = this.nodelist.render(context);
        content = content.trim().replace(/>\s+</g, "><");
        printer(content);
    }
};

var TemplateTagNode =
    defaulttags.TemplateTagNode =
    function(tagtype) {

    this.tagtype = tagtype;
};
//FIXME: these need to be configurable in the parser, and this should be read from the parser
TemplateTagNode.mapping = {
    'openblock': "{%",
    'closeblock': "%}",
    'openvariable': "{{",
    'closevariable': "}}",
    'openbrace': "{",
    'closebrace': "}",
    'opencomment': "{#",
    'closecomment': "#}",  
};
TemplateTagNode.prototype = {
    __proto__: djang10.Node.prototype,
    
    toString: function() {
        return "<TemplateTag Node: '" + this.tagtype + "'>"
    },
    __render: function(context, printer) {
        printer(TemplateTagNode.mapping[this.tagtype]);
    }
};

var WidthRatioNode =
    defaulttags.WidthRatioNode =
    function(val_expr, max_expr, max_width) {

    this.val_expr = val_expr;
    this.max_expr = max_expr;
    this.max_width = max_width;
};
WidthRatioNode.prototype = {
    __proto__: djang10.Node.prototype,
    
    toString: function() {
        return "<WidthRatio Node: val_expr: " + this.val_expr + ", max_expr: " + this.max_expr + ", max_width: " + this.max_width + ">";
    },
    __render: function(context, printer) {
        try {
            var value = parseFloat( this.val_expr.resolve(context) );
            var maxvalue = parseFloat( this.max_expr.resolve(context) );
            var ratio = (value/maxvalue) * parseInt(this.max_width);
            
            if(!isNaN(ratio))            
                printer(Math.round(ratio) );
        }
        catch(e) {
            //fail silently
        }
    }
};


//Registration
var comment =
    defaulttags.comment =
    function(parser, token) {
        
    parser.skip_past("endcomment");
    return new CommentNode();
};
register.tag("comment", comment);

var cycle =
    defaulttags.cycle =
    function(parser, token) {
 
    var args = token.split_contents();
    
    if(args.length < 2)
        throw djang10.NewTemplateException("'cycle' tag requires at least two arguments");


    
    if(args[1].indexOf(',') > -1) {
        var parts = args[1].split(",").map(quote)
        
        args.splice.apply(args, [1,1].concat(parts));
    }
  
    if(args.length == 2) {
        var name = args[1];

        if(!("_namedCycleNodes" in parser))
            throw djang10.NewTemplateException("No named cycles in template. '" + name + "' is not defined");
        return parser["_namedCycleNodes"][name];
    }

    if (args.length > 4 && args[args.length - 2] == "as") {
        var name = args[args.length - 1];


        var cycle_exprs = args.slice(1, -2).map(function(item) { return parser.compile_expression(item); });

        var node = new CycleNode(cycle_exprs, name);
        if (!("_namedCycleNodes" in parser)) 
            parser["_namedCycleNodes"] = {};
        
        parser["_namedCycleNodes"][name] = node;
        return node;
    }
    else {
        var cycle_exprs = args.slice(1).map(function(item) { return  parser.compile_expression(item); });
        var node = new CycleNode(cycle_exprs);
        return node;
    }
}
register.tag("cycle", cycle);


var do_filter =
    defaulttags.do_filter =
    function(parser, token) {
        
    var rest = token.contents.replace(/^\S+\s+/, "");
    var filter_expr = parser.compile_filter("temp|" + rest);
    var nodelist = parser.parse(["endfilter"]);
    parser.delete_first_token();
    return new FilterNode(filter_expr, nodelist);
};
register.tag("filter", do_filter);

var do_for =
    defaulttags.do_for =
    function(parser, token) {
        
    var bits = token.split_contents();
    if(bits.length < 4)
        throw djang10.NewTemplateException("'for' statements should have at least four words: " + token.contents);
    
    var loopvar = bits[1];
    
    var is_reversed = (bits[bits.length - 1] == "reversed");
    if(bits[2] != "in")
        throw djang10.NewTemplateException("'for' statements should use the format 'for x in y': " + token.contents);
    
    var sequenceStr = bits.slice(3, is_reversed? -1:null).join(" ");
    var sequence = parser.compile_filter(sequenceStr);
;    
    var nodelist_loop = parser.parse(["endfor"]);
    parser.delete_first_token();
    
    return new ForNode(loopvar, sequence, is_reversed, nodelist_loop);
};
register.tag("for", do_for);

var do_ifequal =
    defaulttags.do_ifequal =
    function(parser, token, negate) {
        
    var bits = token.split_contents();
    if(bits.length != 3)
        throw djang10.NewTemplateException(bits[0] + " takes two arguments");

    var var1 = parser.compile_expression(bits[1]);
    var var2 = parser.compile_expression(bits[2]);
    
    var end_tag = "end" + bits[0];
    var nodelist_true = parser.parse(["else", "end"+bits[0]]);
    var nodelist_false;
    if(parser.next_token().contents == "else") {
        nodelist_false = parser.parse(["end"+bits[0]]);
        parser.delete_first_token();
    }
    else {
        nodelist_false = parser.create_nodelist();
    }

    return new IfEqualNode(var1, var2, nodelist_true, nodelist_false, negate);
};
var ifequal =
    defaulttags.ifequal = 
    function(parser, token) {

    return do_ifequal(parser, token, false);    
};
register.tag("ifequal", ifequal);

var ifnotequal =
    defaulttags.ifnotequal =
    function(parser, token) {

    return do_ifequal(parser, token, true);        
};
register.tag("ifnotequal", ifnotequal);
  


var firstof =
    defaulttags.firstof =
    function(parser, token) {

    var bits = token.split_contents().slice(1);
    if(bits.length < 1)
        throw djang10.NewTemplateException("'firstof' statement requires at least one argument");
    
    var exprs = bits.map(function(bit) { return parser.compile_expression(bit); });
    return new FirstOfNode(exprs);
};
register.tag("firstof", firstof);

var do_if =
    defaulttags.do_if =
    function(parser, token) {
        
    var bits = token.contents.split(/\s+/);
    bits.shift();
    if(bits.length == 0)
        throw djang10.NewTemplateException("'if' statement requires at least one argument");
    
    var bitstr = "" + bits.join(" ");
    var boolpairs = bitstr.split(" and ");

    var boolvars = [];
    var link_type;
    if(boolpairs.length == 1) {
        link_type = IfNode.LinkTypes.or_;
        boolpairs = bitstr.split(" or ");
    }
    else {
        link_type = IfNode.LinkTypes.and_;
        if(bitstr.indexOf(" or ") > -1)
        throw djang10.NewTemplateException("'if' tags can't mix 'and' and 'or'");
    }
    
    for(var i=0; i<boolpairs.length; i++) {
        var boolpair = boolpairs[i];

        if( boolpair.indexOf(" ") > -1) {
            var boolpair_parts = boolpair.split(" ");
            var not = boolpair_parts[0];
            var boolvar = boolpair_parts[1];
            
            if(not != 'not')
                throw djang10.NewTemplateException("Expected 'not' in if statement");
            boolvars.push(new IfNode.BoolExpr(true, parser.compile_filter(boolvar)));
        }
        else {
            boolvars.push(new IfNode.BoolExpr(false, parser.compile_filter(boolpair)));
        }
    }

    var nodelist_true = parser.parse(["else", "endif"]);
    var nodelist_false;
    var token = parser.next_token();
    if(token.contents == "else") {
        nodelist_false = parser.parse(["endif"]);
        parser.delete_first_token();
    }
    else {
        nodelist_false = parser.create_nodelist();
    }
    
    return new IfNode(boolvars, nodelist_true, nodelist_false, link_type);
};
register.tag("if", do_if);

var ifchanged =
    defaulttags.ifchanged =
    function(parser, token) {

    var bits = token.contents.split(/\s+/);
    var nodelist = parser.parse(["endifchanged"]);
    parser.delete_first_token();
    
    var exprs = [];
    for(var i=1; i<bits.length; i++) {
        exprs.push(parser.compile_expression(bits[i]) );
    }
    
    return new IfChangedNode(nodelist, exprs);
};
register.tag("ifchanged", ifchanged);

var load =
    defaulttags.load =
    function(parser, token) {

    var bits = token.split_contents();
    for(var i=1; i<bits.length; i++) {
        var library_file = djang10.loadLibrary(bits[i]);
        var library = djang10.evalLibrary(library_file);
        
        parser.add_library(library);
        parser.add_dependency(library_file["_jxpSource"]);
    }
    return new LoadNode();
};
register.tag("load", load);


var now =
    defaulttags.now =
    function(parser, token) {

    var bits = token.split_contents();
    if(bits.length != 2)
        throw djang10.NewTemplateException("'now' statement takes one argument");
    var expr = parser.compile_expression(bits[1]);
    
    return new NowNode(expr);
};
register.tag("now", now);

var regroup =
    defaulttags.regroup =
    function(parser, token) {

    var pattern = /^\s*\S+\s+(.+?)\s+by\s+(\S+)\s+as\s+(\S+)\s*$/;
    var match = pattern.exec(token.contents);
    
    if(match == null)
        throw djang10.NewTemplateException("'regroup' tag requires the format: {% regroup list_expression|optional_filter:with_optional_param by prop_name as result_var_name %}. got: " + token);
    
    var list_expr = parser.compile_filter(match[1]);
    var prop_name = match[2];
    var var_name = match[3];
    
    return new RegroupNode(list_expr, prop_name, var_name);
};
register.tag("regroup", regroup);

var spaceless =
    defaulttags.spaceless =
    function(parser, token) {

    var nodelist = parser.parse(["endspaceless"]);
    parser.delete_first_token();
    return new SpacelessNode(nodelist);
};
register.tag("spaceless", spaceless);

var templatetag =
    defaulttags.templatetag =
    function(parser, token) {

    var bits = token.contents.split(/\s+/);

    if(bits.length != 2)
        throw djang10.NewTemplateException("'templatetag' statement takes one argument")
    
    var tag = bits[1];
    if(!(tag in TemplateTagNode.mapping))
        throw djang10.NewTemplateException("Invalid templatetag argument: '"+tag+"'. Must be one of: " + TemplateTagNode.mapping.keySet().join(", "));
    
    return new TemplateTagNode(tag);
};
register.tag("templatetag", templatetag);

var widthratio =
    defaulttags.widthratio =
    function(parser, token) {

    var bits = token.contents.split(/\s+/);
    if(bits.length != 4)
        throw djang10.NewTemplateException("widthratio takes three arguments");
    var this_value_expr = bits[1];
    var max_value_expr = bits[2];
    var max_width = bits[3];
    
    try {
        max_width = parseInt(max_width);
    }
    catch(e) {
        throw djang10.NewTemplateException("widthratio final argument must be an integer");
    }
    
    return new WidthRatioNode(parser.compile_filter(this_value_expr), parser.compile_filter(max_value_expr), max_width);
};
register.tag("widthratio", widthratio);

//private helpers
var quote = function(str) { return '"' + str + '"';};

var isinstance = function(object, constructor){
    while (object != null && (typeof object == "object")) {
        if (object == constructor.prototype) 
            return true;
        object = object.__proto__;
    }
    return false;
};

return defaulttags;
