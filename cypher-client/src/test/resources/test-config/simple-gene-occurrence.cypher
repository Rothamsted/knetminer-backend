MATCH path = 
  (g:Gene) - [occ:cooc_wi] -> (to:Trait)
  - [occ1:cooc_wi] -> (g1:Gene)
  - [part:participates_in] -> (proc:BioProc)
WHERE g.iri IN $startGeneIris
RETURN path
ORDER BY occ.MAX_TFIDF DESC
