import java.util.HashMap;

public class Pieces {
    public static final int MaxOrder = 6;

    public static int Order(Piece piece) {
        return (piece.value >> 2);
    }

    private static final HashMap<Integer, Piece> pieceMap;
    static
    {
        pieceMap=new HashMap<>();
        for(Piece p: Piece.values())
            pieceMap.put(p.value, p);
    }

    public static Piece get(int v)
    {
        return pieceMap.get(v);
    }

    public static Color Flip(Color color) {
        return color==Color.Black?Color.White:Color.Black;
    }
}