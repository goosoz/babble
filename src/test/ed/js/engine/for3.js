var i = 5;
for ( var i=0; i<2; i = i + 1 ){
    var b = i;
    print( b );
}
print( i );

var i = 17;
function foo(){
    var i = 213;
};
foo();
print( i );

var i = 17;
function foo2(){
    i = 213;
};
foo2();
print( i );


function bb( j ){
    for ( var i=0; i<4; i = i + 1 ){
        print( i + " " + j );
    }
}

function a(){
    for ( var i=0; i<4; i = i + 1 ){
        bb( i );
    }
}

a();
