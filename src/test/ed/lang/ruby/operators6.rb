
$ASD = 5
puts $ASD 

class A
  
  self.foo = 1;

  def a
    self.foo ||= 0
    puts self.foo
  end
end

t = A.new
t.a

