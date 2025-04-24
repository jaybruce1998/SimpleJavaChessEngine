import java.util.function.Supplier;

public class IterativeSearch {
    private static final int QUERY_TC_FREQUENCY = 25;
    private static final int MAX_GAIN_PER_PLY = 70;
    private long NodesVisited;
    public long getNodesVisited()
    {
        return NodesVisited;
    }
    private int Depth;

    private int Score;
    public int getScore() {
        return Score;
    }

    public Move[] PrincipalVariation() {
        return _pv;
    }

    public boolean Aborted() {
        return NodesVisited >= _maxNodes || _killSwitch.Get(NodesVisited % QUERY_TC_FREQUENCY == 0);
    }

    public boolean GameOver() {
        return Evaluation.IsCheckmate(Score);
    }

    private final Board _root;
    private Move[] _pv=new Move[0];
    private final KillerMoves _killers;
    private final History _history;
    private KillSwitch _killSwitch;
    private final long _maxNodes;
    private int _mobilityBonus;

    public IterativeSearch(Board board, long maxNodes)
    {
        _root = new Board(board);
        _killers = new KillerMoves(4);
        _history=new History();
        _maxNodes = maxNodes;
    }

    public IterativeSearch(Board board)
    {
        this(board, Long.MAX_VALUE);
    }

    public IterativeSearch(int searchDepth, Board board)
    {
        this(board);
        while (Depth < searchDepth)
            searchDeeper();
    }

    public void searchDeeper() {
        SearchDeeper(null);
    }

    public void SearchDeeper(Supplier<Boolean> killSwitch) {
        _killers.Expand(++Depth);
        _history.Scale();
        StorePVinTT(_pv, Depth);
        _killSwitch = new KillSwitch(killSwitch);
        ScoreMoves sm = EvalPosition(_root, 0, Depth, SearchWindow.infinite());
        Score=sm.score;
        _pv=sm.moves;
    }

    private void StorePVinTT(Move[] pv, int depth)
    {
        Board position = new Board(_root);
        for (int ply = 0; ply < pv.length; ply++)
        {
            Move move = pv[ply];
            //refactor?
            Transpositions.Store(position.getZobristHash(), --depth, ply, SearchWindow.infinite(), Score, move);
            position.Play(move);
        }
    }

    private ScoreMoves EvalPositionTT(Board position, int ply, int depth, SearchWindow window)
    {
        int[] ttscore=new int[1];
        if (Transpositions.GetScore(position.getZobristHash(), depth, ply, window, ttscore))
            return new ScoreMoves(ttscore[0], new Move[0]);

        ScoreMoves r = EvalPosition(position, ply, depth, new SearchWindow(window));
        if(!Aborted())
            Transpositions.Store(position.getZobristHash(), depth, ply, window, r.score, r.moves.length>0?r.moves[0]:null);//refactor?
        return r;
    }

    private ScoreMoves EvalPosition(Board position, int ply, int depth, SearchWindow window)
    {
        if (depth <= 0)
        {
            _mobilityBonus = Evaluation.ComputeMobility(position);
            return new ScoreMoves(QEval(position, ply, window), new Move[0]);
        }

        NodesVisited++;
        if (Aborted())
            return new ScoreMoves(0, new Move[0]);

        Color color = position.getSideToMove();
        boolean isChecked = position.IsChecked(color);
        //if the previous iteration found a mate we check the first few plys without null move to try and find the shortest mate or escape
        boolean allowNullMove = !Evaluation.IsCheckmate(Score) || (ply > Depth / 4);

        //should we try null move pruning?
        if (allowNullMove && depth >= 2 && !isChecked && window.CanFailHigh(color))
        {
            final int R = 2;
            SearchWindow beta = window.GetUpperBound(color);
            //skip making a move
            Board nullChild = Playmaker.PlayNullMove(position);
            int score = EvalPositionTT(nullChild, ply + 1, depth - R - 1, beta).score;
            //is the evaluation "too good" despite null-move? then don't waste time on a branch that is likely going to fail-high
            //if the static eval look much worse the alpha also skip it
            if (window.FailHigh(score, color))
                return new ScoreMoves(score, new Move[0]);
        }

        //do a regular expansion...
        Move[] pv=new Move[0];
        int expandedNodes=0;
        for (MoveBoard mb: Playmaker.Play(position, depth, _killers, _history))
        {
            Move move=mb.move;
            Board child=mb.board;
            expandedNodes++;
            boolean interesting = expandedNodes == 1 || isChecked || child.IsChecked(child.getSideToMove());

            //some near the leaves that appear hopeless can be skipped without evaluation
            if (depth <= 4 && !interesting)
            {
                //if the static eval look much worse the alpha also skip it
                int futilityMargin = color.value * depth * MAX_GAIN_PER_PLY;
                if (window.FailLow(child.Score() + futilityMargin, color))
                    continue;
            }
            if (depth >= 2 && expandedNodes > 1)
            {
                //non-tactical late moves are searched at a reduced depth to make this test even faster!
                int R = (interesting || expandedNodes < 4) ? 0 : 2;
                int score = EvalPositionTT(child, ply + 1, depth - R - 1, window.GetLowerBound(color)).score;
                if (window.FailLow(score, color))
                    continue;
            }
            var eval = EvalPositionTT(child, ply + 1, depth - 1, new SearchWindow(window));
            if (window.FailLow(eval.score, color))
            {
                _history.Bad(position, move, depth);
                continue;
            }
            pv = Merge(move, eval.moves);
            //...and maybe we even get a beta cutoff
            if (window.Cut(eval.score, color))
            {
                //we remember killers like hat!
                if (position.get(move.ToSquare) == Piece.None)
                {
                    _history.Good(position, move, depth);
                    _killers.Add(move, depth);
                }

                return new ScoreMoves(GetScore(window, color), pv);
            }
        }

        //no playable moves in this position?
        if (expandedNodes == 0)
            return new ScoreMoves(position.IsChecked(color) ? Evaluation.Checkmate(color, ply) : 0, new Move[0]);

        return new ScoreMoves(GetScore(window, color), pv);
    }

    private static int GetScore(SearchWindow window, Color color)
    {
        return window.GetScore(color);
    }

    private static Move[] Merge(Move move, Move[] pv)
    {
        Move[] result = new Move[pv.length + 1];
        result[0] = move;
        System.arraycopy(pv, 0, result, 1, pv.length);
        return result;
    }

    private int QEval(Board position, int ply, SearchWindow window)
    {
        NodesVisited++;
        if (Aborted())
            return 0;
        Color color = position.getSideToMove();

        //if inCheck we can't use standPat, need to escape check!
        boolean inCheck = position.IsChecked(color);
        if (!inCheck)
        {
            int standPatScore = position.Score() + _mobilityBonus;
            //Cut will raise alpha and perform beta cutoff when standPatScore is too good
            if (window.Cut(standPatScore, color))
                return GetScore(window, color);
        }

        int expandedNodes = 0;
        //play remaining captures (or any moves if king is in check)
        for (Board child: inCheck ? Playmaker.Play(position) : Playmaker.PlayCaptures(position))
        {
            expandedNodes++;
            //recursively evaluate the resulting position (after the capture) with QEval
            int score = QEval(child, ply+1, new SearchWindow(window));

            //Cut will raise alpha and perform beta cutoff when the move is too good
            if (window.Cut(score, color))
                break;
        }

        //checkmate?
        if (expandedNodes == 0 && inCheck)
            return Evaluation.Checkmate(color, ply);

        //stalemate?
        if (expandedNodes == 0 && !LegalMoves.HasMoves(position))
            return 0;

        //can't capture. We return the 'alpha' which may have been raised by "stand pat"
        return GetScore(window, color);
    }
}