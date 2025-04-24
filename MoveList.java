import java.util.ArrayList;
import java.util.Collections;

public class MoveList extends ArrayList<SortedMove>
{
    static MoveList Quiets(Board position)
    {
        MoveList quietMoves = new MoveList();
        position.CollectQuiets(m->quietMoves.Add(m, 0));
        return quietMoves;
    }

    static MoveList SortedCaptures(Board position)
    {
        MoveList captures = new MoveList();
        position.CollectCaptures(m->captures.Add(m, ScoreMvvLva(m, position)));
        Collections.sort(captures);
        return captures;
    }

    public static MoveList SortedQuiets(Board position, History history)
    {
        MoveList quiets = new MoveList();
        position.CollectQuiets(m -> quiets.Add(m, history.Value(position, m)));
        Collections.sort(quiets);
        return quiets;
    }

    private static int ScoreMvvLva(Move move, Board context)
    {
        Piece victim = context.get(move.ToSquare);
        Piece attacker = context.get(move.FromSquare);
        return Pieces.MaxOrder * Pieces.Order(victim) - Pieces.Order(attacker);
    }

    private void Add(Move move, float priority) {
        add(new SortedMove(move, priority));
    }
}
