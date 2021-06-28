module Ex1 (ListBag (..), wf, empty, singleton, fromList, isEmpty, mul, toList, sumBag) where
import Data.Maybe (fromMaybe, isJust)

{-
  @author Marco Costa
  ListBag implements a MultiSet, where each element is formed by a pair 
    whose first component is the actual element of the multiset, 
    and the second component is its multiplicity.
  All the constructors and the operations maintain the well-formedness of
    the structure as required in the exercise text.
-}
data ListBag a = LB [(a, Int)] deriving (Show, Eq)

-- In order to check well-formedness we apply recursively for each element in the ListBag
-- a check: if the value is <= 0 or the element is already present in the rest of the list
-- the argument is *not* well-formed
wf :: Eq a => ListBag a -> Bool
wf (LB []) = True
wf (LB ((a, b) : xs))
    | b <= 0 = False
    | isJust (lookup a xs) = False -- lookup for the same element in the set (lookup returns a Maybe)
    | otherwise = wf (LB xs)
    
empty :: ListBag a
empty = LB[]

-- Creates a new ListBag with a single element and multiplicity one
singleton :: a -> ListBag a
singleton v = LB [(v, 1)]

-- Adds a tuple (x, y) as head of a ListBag object
-- Note: this function must remain private (for this reason is not exported)
--       since it must be used into a function which preserves well-formedness
app :: (a, Int) -> ListBag a -> ListBag a
app (x, y) (LB a) = LB ((x, y) : a)

-- Constructs a new ListBag object from a List of objects
-- for each object in the list, it appends on a new ListBag object
-- the tuple (x, y) where y is given by the length of the list filtered by x
-- fromList is then applied recursively on the next element of the input list
-- removed by the next occurrences of x
fromList :: Eq a => [a] -> ListBag a
fromList [] = empty
fromList [x] = singleton x
fromList (x : xs) = (x, (length . filter (== x) $ xs) + 1) `app` fromList (filter (/= x) xs)

-- Checks if a ListBag object is empty or not
isEmpty :: ListBag a -> Bool
isEmpty (LB []) = True
isEmpty _ = False

-- Returns the multiplicity of an object in a ListBag, 0 if not in the set 
mul :: Eq a => a -> ListBag a -> Int
--- The fromMaybe function takes a default value and a Maybe value. If the Maybe is Nothing,
--- it returns the default value; otherwise, it returns the value contained in the Maybe.
mul v (LB x) = fromMaybe 0 (lookup v x)

-- Returns as a List of objects the elements of a ListBag respecting the multiplicities
-- described in the set
toList :: ListBag a -> [a]
toList (LB []) = []
toList (LB ((x, y) : xs)) = replicate y x ++ toList (LB xs) -- list concatenation

-- Sum two ListBag objects and their multiplicities in case of equivalent elements
-- the method starts by scanning the elements (x, y) of the first list and appends on a new ListBag object the tuple
-- (x, y') where y' is given by 'y' plus the multiplicity of x in the second list, if any.
-- Then, the method is applied recursively on the first list truncated by its first element and the second one
-- filtered by the occurrence of x. When the first list ends the remaining second list is appended.
sumBag :: Eq a => ListBag a -> ListBag a -> ListBag a
sumBag (LB []) (LB []) = LB []
sumBag (LB []) (LB l) = LB l
sumBag (LB ((x, y) : xs)) (LB l) = (x, y + fromMaybe 0 (lookup x l)) `app` sumBag (LB xs) (LB (filter ((/= x) . fst) l))
