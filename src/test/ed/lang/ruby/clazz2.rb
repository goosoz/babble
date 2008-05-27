
class Foo
  Z = /^(#{
        ['*', '-', '_'].collect { |ch| '( ?' + Regexp::quote( ch ) + ' ?){3,}' }.join( '|' )
    })$/

  TEXTILE_TAGS =
    
    [[128, 8364], [129, 0], [130, 8218], [131, 402], [132, 8222], [133, 8230], 
     [134, 8224], [135, 8225], [136, 710], [137, 8240], [138, 352], [139, 8249], 
     [140, 338], [141, 0], [142, 0], [143, 0], [144, 0], [145, 8216], [146, 8217], 
     [147, 8220], [148, 8221], [149, 8226], [150, 8211], [151, 8212], [152, 732], 
     [153, 8482], [154, 353], [155, 8250], [156, 339], [157, 0], [158, 0], [159, 376]].
    
    collect! do |a, b|
    [a.chr, ( b.zero? and "" or "&#{ b };" )]
  end
  
  SILLY = Regexp.quote( "*" )

end



