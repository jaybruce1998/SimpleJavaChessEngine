import java.util.ArrayList;

public class LegalMoves extends ArrayList<Move>
{
    private static final Board _tempBoard = new Board();

    public LegalMoves(Board reference)
    {
        super(40);
        reference.CollectMoves(move ->
        {
            //only add if the move doesn't result in a check for active color
            _tempBoard.Copy(reference);
            _tempBoard.Play(move);
            if (!_tempBoard.IsChecked(reference.getSideToMove()))
                add(move);
        });
    }

    public static boolean HasMoves(Board position)
    {
        final boolean[] canMove = {false};
        for (int i = 0; i < 64 && !canMove[0]; i++)
        {
            position.CollectMoves(i, move ->
            {
                if (canMove[0]) return;

                _tempBoard.Copy(position);
                _tempBoard.Play(move);
                canMove[0] = !_tempBoard.IsChecked(position.getSideToMove());
            });
        }
        return canMove[0];
    }
}

