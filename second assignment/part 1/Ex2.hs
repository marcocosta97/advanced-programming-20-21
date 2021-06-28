{-# LANGUAGE InstanceSigs #-} -- allows type signature in instance declaration
import Ex1

{-1-}

-- Instance of the Foldable constructor class for ListBag
-- according to the documentation of Foldable the minimal set of function
--   to be implemented is either foldMap or foldr.
instance Foldable ListBag where
   foldr :: (a -> b -> b) -> b -> ListBag a -> b
   foldr f z (LB []) = z -- z is the `base case` element
   foldr f z (LB ((x, y): xs)) = f x (foldr f z (LB xs))

{-2-}

-- mapLB returns the ListBag of type b obtained by applying f to all the 
-- elements of its second argument
mapLB :: Eq b => (a -> b) -> ListBag a -> ListBag b
mapLB f (LB t) = LB [(f x, y) | (x, y) <- t] -- easy using list comprehension over the ListBag
    

{-3-}

{-
    mapLB cannot be used as a valid implementation of `fmap` for, at least, two reasons:
    1. There is no guarantee that the function passed as parameter would produce a well-formed
       new ListBag object. For example the `func t =  'a'` would be a valid function which, 
       passed to `mapLB` transforms all the keys in the set in 'a' characters returning 
       a ListBag Char, but the resulting object would not be well-formed.
    2. The type of `mapLB` and `fmap` differs. This is due to the fact that `mapLB` must
       requests that the type `b` is instance of the type `Eq`. If not the case, it
       would not be possible, for example, to lookup elements inside the ListBag object.
       Conversely, the type of `fmap` does not require this constraint, in fact, 
       trying to do the following
       
       instance Functor ListBag where
          fmap = mapLB

       results in a error from the compiler (No instance for (Eq b) arising from a use of ‘mapLB’)
-}
