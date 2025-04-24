import java.util.function.Supplier;

public class KillSwitch {
    Supplier<Boolean> _killSwitch;//refactor?
    boolean _aborted;

    public KillSwitch(Supplier<Boolean> killSwitch) {
        _killSwitch = killSwitch;
        _aborted = _killSwitch != null && _killSwitch.get();
    }

    public boolean Get(boolean update)
    {
        if (!_aborted && update && _killSwitch != null)
            _aborted = _killSwitch.get();
        return _aborted;
    }
}