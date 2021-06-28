
import functools
import time
import logging
import math
import threading


def benchmark(warmups: int = 0, iter: int = 1, verbose=False, csv_file: str = None):
    """Decorator for function benchmarking in Python.

    Executes a function, possibly multiple times, discarding the results and printing
    a small table with the resulting times and other info. 
    Args:
        warmups (int, optional): the number of warmups iterations. Defaults to 0.
        iter (int, optional): the number of iterations to average. Defaults to 1.
        verbose (bool, optional): prints the timings for both warmups and standard iterations. Defaults to False.
        csv_file (str, optional): if defined prints the results in a csv file. Defaults to None.
    """
    def benchmark_decorator(func):

        # Checking the arguments correctness
        if(warmups < 0):
            raise ValueError('`warmups` argument is invalid (must be >= 0)')
        if(iter <= 0):
            raise ValueError('`iter` argument is invalid (must be >= 0)')
        if(not csv_file.endswith('.csv')):
            raise ValueError('`csv_file` is not a valid csv filename')

        # Use of @functools.wraps in order to copy the original function attributes
        @functools.wraps(func)
        def wrapper_benchmark(*args, **kwargs):

            # if `verbose` is True we set the logging level to INFO instead than
            # the default on WARNING
            if verbose:
                logging.basicConfig(
                    format='%(levelname)s: %(message)s', level=logging.INFO)
            else:
                logging.getLogger().setLevel(level=logging.WARNING)

            name = func.__name__
            arg_str = ', '.join(repr(arg) for arg in args)
            fname = name + '(' + arg_str + ')'
            print(f'\n_______BENCHMARK___{fname}_______\n')

            # Wraps the execution measuring into a function
            def run(is_warmup=False):
                start_time = time.perf_counter()
                func(*args, **kwargs)
                elapsed = time.perf_counter() - start_time
                if is_warmup:
                    logging.info('warmup: %d) executed %s in [%0.8fs]' % (
                        i + 1, fname, elapsed))
                else:
                    logging.info('iteration: %d) executed %s in [%0.8fs]' % (
                        i + 1, fname, elapsed))
                return elapsed

            # Starting the warmups iterations
            warmup_times = []
            for i in range(warmups):
                warmup_times.append(run(is_warmup=True))

            # Starting the timed iterations
            times = []
            for i in range(iter):
                times.append(run())

            # Computing the max, min, average, variance and stdev in a
            # very inefficient way, with multiple passes over the data
            # (affordable since the number of iterations is small)
            time_min = min(times)
            time_max = max(times)
            avg = sum(times)/iter
            var_list = [(x - avg) ** 2 for x in times] # Var(X) = [sum (x - avg)^2]/(n)
            var = sum(var_list)/iter
            stdev = math.sqrt(var) # Stdev(x) = sqrt(x)

            # Printing the results in a table
            columns = ["Name", "#IT", "#WU",
                       "MIN(s)", "MAX(s)", "AVG(s)", "Var", "StDev"]
            values = [name + '(' + arg_str + ')', iter, warmups, time_min,
                      time_max, avg, var, stdev]
            print(
                "\n{:^16s}|{:^5s}|{:^5s}|{:^8s}|{:^8s}|{:^8s}|{:^10s}|{:^10s}".format(*columns))
            print(
                "--------------------------------------------------------------------------------")
            print("{:^16s}|{:^5d}|{:^5d}|{:^8.4f}|{:^8.4f}|{:^8.4f}|{:^10f}|{:^10f}".format(
                *values))

            # If the `csv_file` string is specified prints the computed times
            # in the specified csv file
            if csv_file:
                source = open(csv_file, 'w')
                print('run num, is warmup, timing', file=source)
                for i in range(warmups):
                    print('{0}, y, {1}'.format(
                        i + 1, warmup_times[i]), file=source)
                for i in range(iter):
                    print('{0}, n, {1}'.format(i + 1, times[i]), file=source)
                source.close()

            print('\n_______END_OF_BENCHMARK_______\n')

        return wrapper_benchmark
    return benchmark_decorator

def test(func):
    """Tests the Python 'threading' module.
    Executes a function `func` passed as a parameter using:
        1 thread and 16 iterations
        2 threads and 8 iterations
        4 threads and 4 iterations
        8 threads and 2 iterations
    printing the resulting benchmarks on screen and on multiple csv files.

    Args:
        func ([type]): the function to be executed
    """
    def func_run(n_threads, n_iter):
        print(f"Running {func.__name__} with {n_threads} thread and {n_iter} iterations")
        @benchmark(iter=n_iter, csv_file=f"f_{n_threads}_{n_iter}.csv")
        def wrapper():
            def iterate_f():
                for _ in range(n_iter):
                    func()
            threads = [threading.Thread(target=iterate_f)
                       for _ in range(n_threads)]

            for t in threads:
                t.start()
            for t in threads:
                t.join()

        return wrapper

    func_run(n_threads=1, n_iter=16)()
    func_run(n_threads=2, n_iter=8)()
    func_run(n_threads=4, n_iter=4)()
    func_run(n_threads=8, n_iter=2)()


def fib(n=22):
    if n <= 1:
        return 1
    else:
        return fib(n - 1) + fib(n - 2)

@benchmark(warmups=1, iter=5, verbose=True, csv_file="out.csv")
def waste_time(num):
    for _ in range(num):
        sum([i**2 for i in range(10000)])
    return num

#waste_time(20)

if __name__ == "__main__":
    test(fib)

"""
The results of the execution of the Fibonacci function (with n = 22) with multithreading using the 'threading'
module show what we expected: no benefits running parallel execution in Python (more precisely in CPython) due to the presence of
the Global Interpreter Lock which allows only one thread at the same time to hold the control of the Python interpreter.
Furthermore, increasing the number of threads shows a slight directly proportional increasing 
over the average computation time; this is probably the effect of the overhead caused by the creation execution and manage of the threads
(which explains why adding threads slows more the computation).

Results:

    _______BENCHMARK___wrapper()_______

        Name      | #IT | #WU | MIN(s) | MAX(s) | AVG(s) |   Var    |  StDev   
    --------------------------------------------------------------------------------
    wrapper()    | 16  |  0  | 0.0858 | 0.0972 | 0.0893 | 0.000012 | 0.003494 
    wrapper()    |  8  |  0  | 0.0888 | 0.0994 | 0.0908 | 0.000011 | 0.003299 
    wrapper()    |  4  |  0  | 0.1136 | 0.1222 | 0.1172 | 0.000012 | 0.003464 
    wrapper()    |  2  |  0  | 0.1155 | 0.1302 | 0.1228 | 0.000054 | 0.007375 

    _______END_OF_BENCHMARK_______

"""
